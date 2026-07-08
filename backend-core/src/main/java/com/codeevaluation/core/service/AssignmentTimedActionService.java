package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.run.RunBatchResponseDto;
import com.codeevaluation.core.api.dto.run.TestCase;
import com.codeevaluation.core.api.dto.run.TestRunResult;
import com.codeevaluation.core.client.plagscan.dto.PlagscanBaseCode;
import com.codeevaluation.core.client.plagscan.dto.PlagscanFilePayload;
import com.codeevaluation.core.client.plagscan.dto.PlagscanResult;
import com.codeevaluation.core.client.plagscan.dto.PlagscanScanRequest;
import com.codeevaluation.core.config.AssignmentConfig;
import com.codeevaluation.core.enumeration.SubmissionStatus;
import com.codeevaluation.core.enumeration.SubmissionTestRunStatus;
import com.codeevaluation.core.enumeration.TestResult;
import com.codeevaluation.core.enumeration.TestVisibility;
import com.codeevaluation.core.helper.TestResultResolver;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.GroupMember;
import com.codeevaluation.core.model.Submission;
import com.codeevaluation.core.model.SubmissionCluster;
import com.codeevaluation.core.model.SubmissionClusterMember;
import com.codeevaluation.core.model.SubmissionFile;
import com.codeevaluation.core.model.SubmissionPlagiarismRun;
import com.codeevaluation.core.model.SubmissionSimilarity;
import com.codeevaluation.core.model.SubmissionTestResult;
import com.codeevaluation.core.model.SubmissionTestRun;
import com.codeevaluation.core.model.TaskTest;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.repository.AssignmentRepository;
import com.codeevaluation.core.repository.SubmissionPlagiarismRunRepository;
import com.codeevaluation.core.repository.SubmissionRepository;
import com.codeevaluation.core.service.dto.QuartzOneTimeJobRequest;
import com.codeevaluation.core.service.dto.RunResult;
import com.codeevaluation.core.util.DurationUtil;
import com.codeevaluation.core.util.FileUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class AssignmentTimedActionService {

    private static final String DEFAULT_SUBMISSION_FILE_ID = "src/main.cpp";
    private static final String STARTER_CODE_FILE_ID = "starter.cpp";

    private final AssignmentConfig assignmentConfig;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionPlagiarismRunRepository submissionPlagiarismRunRepository;
    private final CodeExecutionService codeExecutionService;
    private final PlagscanService plagscanService;
    private final MailService mailService;
    private final Instance<AssignmentTimedActionService> self;
    private final QuartzSchedulerService quartzSchedulerService;
    private final EntityManager entityManager;

    public void scheduleOnCreated(Long assignmentId, Instant startsAt, Instant endsAt) {
        scheduleStartReminderOrSendImmediately(assignmentId, startsAt);
        schedulePostAction(assignmentId, endsAt);
    }

    @Transactional
    public void sendStartReminder(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithReminderRelations(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found for reminder: " + assignmentId));

        List<String> recipients = assignment.getGroup().getMembers().stream()
                .map(GroupMember::getUser)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .map(User::getEmail)
                .distinct()
                .toList();

        mailService.sendAssignmentStartReminder(recipients, assignment);
    }

    public void executePostAction(Long assignmentId) {
        PostActionPlan plan = self.get().loadPostActionPlan(assignmentId);
        if (plan.submissions().isEmpty()) {
            log.info("Assignment post-action skipped because assignment has no submissions, "
                            + "assignmentId={}",
                    assignmentId);
            return;
        }

        for (SubmissionSnapshot submission : plan.pendingCodeRunSubmissions()) {
            RunBatchResponseDto runResult = executeWithBackpressureRetry(
                    "code run for submissionId=" + submission.id(),
                    () -> codeExecutionService.runBatch(submission.code(), plan.toTestCases())
            );
            self.get().recordCodeRun(plan, submission.id(), runResult);
        }

        if (plan.plagiarismAlreadyRecorded()) {
            log.info("Assignment plagiarism scan already recorded for assignmentId={}",
                    assignmentId);
            return;
        }
        if (plan.submissions().size() < 2) {
            log.info("Assignment plagiarism scan skipped because fewer than two submissions "
                    + "exist, assignmentId={}", assignmentId);
            return;
        }

        PlagscanResult plagscanResult = executeWithBackpressureRetry(
                "plagscan for assignmentId=" + assignmentId,
                () -> plagscanService.scanCpp(plan.toPlagscanRequest())
        );
        self.get().recordPlagscanResult(plan, plagscanResult);

        log.info("Assignment post-action completed for assignmentId={}", assignmentId);
    }

    @Transactional
    public PostActionPlan loadPostActionPlan(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithTaskAndTests(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found for post-action: " + assignmentId));
        List<Submission> submissions = submissionRepository.findByAssignmentIdWithFiles(
                assignmentId);

        List<TestCaseSnapshot> tests = assignment.getTask().getTests().stream()
                .sorted(Comparator.comparing(TaskTest::getId))
                .map(test -> new TestCaseSnapshot(
                        test.getId(),
                        test.getInput(),
                        test.getOutput(),
                        test.getVisibility()
                ))
                .toList();

        List<SubmissionSnapshot> submissionSnapshots = submissions.stream()
                .map(this::toSubmissionSnapshot)
                .toList();
        List<SubmissionSnapshot> pendingCodeRunSubmissions = submissions.stream()
                .filter(this::shouldRunCode)
                .map(this::toSubmissionSnapshot)
                .toList();

        boolean plagiarismAlreadyRecorded = submissionPlagiarismRunRepository
                .count("assignment.id", assignmentId) > 0;

        return new PostActionPlan(
                assignment.getId(),
                assignment.getTask().getId(),
                assignment.getPoints(),
                assignment.getTask().getStarterCode(),
                Boolean.TRUE.equals(assignment.getTask().getIncludeStarterCode()),
                tests,
                submissionSnapshots,
                pendingCodeRunSubmissions,
                plagiarismAlreadyRecorded,
                assignmentConfig.postAction().plagscanMinSimilarity()
        );
    }

    @Transactional
    public void recordCodeRun(
            PostActionPlan plan,
            Long submissionId,
            RunBatchResponseDto runBatchResponse
    ) {
        Submission submission = submissionRepository.findByIdOptional(submissionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Submission not found for post-action: " + submissionId));

        SubmissionTestRun testRun = new SubmissionTestRun();
        testRun.setTotalTests(plan.tests().size());
        testRun.setRuntimeMs(totalRuntimeMs(runBatchResponse));
        testRun.setLogOutput(compileLog(runBatchResponse));

        RunResult compile = runBatchResponse.getCompile();
        if (compile == null || compile.getExitCode() != 0 || compile.isTimedOut()) {
            testRun.setStatus(SubmissionTestRunStatus.FAILED);
            testRun.setPassedTests(0);
            submission.addTestRun(testRun);
            submission.setStatus(SubmissionStatus.FAILED);
            submission.setFinalScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return;
        }

        Map<Integer, TestRunResult> resultsByIndex = new HashMap<>();
        if (runBatchResponse.getResults() != null) {
            runBatchResponse.getResults().forEach(result -> resultsByIndex.put(
                    result.getIndex(), result));
        }

        int passedTests = 0;
        for (int i = 0; i < plan.tests().size(); i++) {
            TestCaseSnapshot test = plan.tests().get(i);
            TestRunResult runResult = resultsByIndex.get(i);
            TestResult resolvedResult = TestResultResolver.resolveFor(
                    test.expectedOutput(), runResult);
            if (resolvedResult == TestResult.PASSED) {
                passedTests++;
            }

            SubmissionTestResult testResult = new SubmissionTestResult();
            testResult.setTaskTest(entityManager.getReference(TaskTest.class, test.id()));
            testResult.setResult(resolvedResult);
            testResult.setVisibility(test.visibility());
            testResult.setTestInput(test.input());
            testResult.setExpectedOutput(test.expectedOutput());
            testResult.setActualOutput(runResult == null ? null : runResult.getStdout());
            testResult.setRuntimeMs(runResult == null ? null : runResult.getDurationMs());
            testResult.setErrorOutput(
                    runResult == null ? "Missing run result" : runResult.getStderr());
            testRun.addTestResult(testResult);
        }

        testRun.setStatus(SubmissionTestRunStatus.COMPLETED);
        testRun.setPassedTests(passedTests);
        submission.addTestRun(testRun);
        submission.setStatus(SubmissionStatus.TESTED);
        submission.setFinalScore(calculateScore(passedTests, plan.tests().size(),
                plan.assignmentPoints()));
    }

    @Transactional
    public void recordPlagscanResult(PostActionPlan plan, PlagscanResult result) {
        if (submissionPlagiarismRunRepository.count("assignment.id", plan.assignmentId()) > 0) {
            log.info("Skipping duplicate plagiarism result for assignmentId={}",
                    plan.assignmentId());
            return;
        }

        SubmissionPlagiarismRun plagiarismRun = new SubmissionPlagiarismRun();
        plagiarismRun.setAssignment(
                entityManager.getReference(Assignment.class, plan.assignmentId())
        );
        plagiarismRun.setTask(entityManager.getReference(com.codeevaluation.core.model.Task.class,
                plan.taskId()));
        plagiarismRun.setMinSimilarity(toSimilarity(result.minSimilarity()));
        plagiarismRun.setReportFileBase64(StringUtils.defaultString(result.fileBase64()));

        Map<String, Long> submissionIdsByPlagscanId = plan.submissions().stream()
                .collect(HashMap::new,
                        (map, submission) -> map.put(submission.plagscanId(), submission.id()),
                        HashMap::putAll);

        if (result.pairs() != null) {
            result.pairs().forEach(pair -> {
                Long sourceId = submissionIdsByPlagscanId.get(pair.studentA());
                Long targetId = submissionIdsByPlagscanId.get(pair.studentB());
                if (sourceId == null || targetId == null) {
                    log.warn("Skipping unknown plagscan pair result: studentA={}, studentB={}",
                            pair.studentA(), pair.studentB());
                    return;
                }

                SubmissionSimilarity similarity = new SubmissionSimilarity();
                similarity.setSourceSubmission(
                        entityManager.getReference(Submission.class, sourceId));
                similarity.setTargetSubmission(
                        entityManager.getReference(Submission.class, targetId));
                similarity.setSimilarityScore(toSimilarity(pair.similarity()));
                plagiarismRun.addSimilarity(similarity);
            });
        }

        if (result.clusters() != null) {
            result.clusters().forEach(clusterResult -> {
                SubmissionCluster cluster = new SubmissionCluster();
                cluster.setSimilarity(toSimilarity(clusterResult.similarity()));

                if (clusterResult.members() != null) {
                    clusterResult.members().forEach(member -> {
                        Long submissionId = submissionIdsByPlagscanId.get(member);
                        if (submissionId == null) {
                            log.warn("Skipping unknown plagscan cluster member: member={}", member);
                            return;
                        }
                        SubmissionClusterMember clusterMember = new SubmissionClusterMember();
                        clusterMember.setSubmission(entityManager.getReference(
                                Submission.class, submissionId));
                        cluster.addMember(clusterMember);
                    });
                }

                plagiarismRun.addCluster(cluster);
            });
        }

        submissionPlagiarismRunRepository.persist(plagiarismRun);
        plan.submissions().forEach(
                submission -> submissionRepository.findByIdOptional(submission.id())
                .ifPresent(existing -> existing.setStatus(SubmissionStatus.PLAGIARISM_ANALYZED)));
    }

    private void scheduleStartReminderOrSendImmediately(Long assignmentId, Instant startsAt) {
        Instant reminderAt = startsAt.minus(reminderLeadTime());
        if (!reminderAt.isAfter(Instant.now())) {
            log.info("Assignment reminder will be sent immediately for assignmentId={}",
                    assignmentId);
            self.get().sendStartReminder(assignmentId);
            return;
        }

        quartzSchedulerService.scheduleOneTimeJob(
                QuartzOneTimeJobRequest.assignmentStart(assignmentId, reminderAt)
        );
    }

    private void schedulePostAction(Long assignmentId, Instant endsAt) {
        Instant executeAt = endsAt.plus(postActionDelay());
        quartzSchedulerService.scheduleOneTimeJob(
                QuartzOneTimeJobRequest.assignmentEnd(assignmentId, executeAt)
        );
    }

    private Duration reminderLeadTime() {
        return DurationUtil.parseFlexibleDuration(assignmentConfig.reminder().leadTime());
    }

    private Duration postActionDelay() {
        return DurationUtil.parseFlexibleDuration(assignmentConfig.postAction().delay());
    }

    private Duration postActionRetryDelay() {
        return DurationUtil.parseFlexibleDuration(assignmentConfig.postAction().retryDelay());
    }

    private boolean shouldRunCode(Submission submission) {
        return submission.getStatus() == SubmissionStatus.SUBMITTED
                || submission.getStatus() == SubmissionStatus.QUEUED;
    }

    private SubmissionSnapshot toSubmissionSnapshot(Submission submission) {
        SubmissionFile file = submission.getFiles().stream()
                .filter(submissionFile -> DEFAULT_SUBMISSION_FILE_ID.equals(
                        submissionFile.getFilePath()))
                .findFirst()
                .orElseGet(() -> submission.getFiles().getFirst());
        return new SubmissionSnapshot(
                submission.getId(),
                submission.getId().toString(),
                file.getContent(),
                file.getContentBase64()
        );
    }

    private <T> T executeWithBackpressureRetry(String action, Supplier<T> supplier) {
        int maxAttempts = Math.max(1, assignmentConfig.postAction().maxAttempts());
        Duration retryDelay = postActionRetryDelay();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (WebApplicationException e) {
                if (!isTooManyRequests(e) || attempt == maxAttempts) {
                    throw e;
                }

                log.warn("Post-action backpressure while executing {}, attempt={}/{}. "
                                + "Retrying in {}", action, attempt, maxAttempts,
                        DurationUtil.toHumanReadable(retryDelay));
                sleep(retryDelay);
            }
        }

        throw new IllegalStateException("Post-action retry loop exhausted for " + action);
    }

    private boolean isTooManyRequests(WebApplicationException exception) {
        Response response = exception.getResponse();
        return response != null
                && response.getStatus() == Response.Status.TOO_MANY_REQUESTS.getStatusCode();
    }

    private void sleep(Duration retryDelay) {
        try {
            Thread.sleep(Math.max(1L, retryDelay.toMillis()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for post-action retry", e);
        }
    }

    private long totalRuntimeMs(RunBatchResponseDto runBatchResponse) {
        if (runBatchResponse == null || runBatchResponse.getResults() == null) {
            return 0L;
        }
        return runBatchResponse.getResults().stream()
                .mapToLong(TestRunResult::getDurationMs)
                .sum();
    }

    private String compileLog(RunBatchResponseDto runBatchResponse) {
        if (runBatchResponse == null || runBatchResponse.getCompile() == null) {
            return "Missing compile result";
        }

        RunResult compile = runBatchResponse.getCompile();
        return "phase=%s exitCode=%d timedOut=%s stdout=%s stderr=%s".formatted(
                compile.getPhase(),
                compile.getExitCode(),
                compile.isTimedOut(),
                StringUtils.defaultString(compile.getStdout()),
                StringUtils.defaultString(compile.getStderr())
        );
    }

    private BigDecimal calculateScore(int passedTests, int totalTests, int assignmentPoints) {
        if (totalTests <= 0 || assignmentPoints <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(assignmentPoints)
                .multiply(BigDecimal.valueOf(passedTests))
                .divide(BigDecimal.valueOf(totalTests), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal toSimilarity(Double value) {
        return BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, value == null ? 0.0 : value)))
                .setScale(11, RoundingMode.HALF_UP);
    }

    private BigDecimal toSimilarity(double value) {
        return toSimilarity(Double.valueOf(value));
    }

    public record PostActionPlan(
            Long assignmentId,
            Long taskId,
            int assignmentPoints,
            String starterCode,
            boolean includeStarterCode,
            List<TestCaseSnapshot> tests,
            List<SubmissionSnapshot> submissions,
            List<SubmissionSnapshot> pendingCodeRunSubmissions,
            boolean plagiarismAlreadyRecorded,
            Double plagscanMinSimilarity
    ) {
        List<TestCase> toTestCases() {
            return tests.stream()
                    .map(test -> new TestCase(test.input()))
                    .toList();
        }

        PlagscanScanRequest toPlagscanRequest() {
            List<PlagscanFilePayload> payloads = submissions.stream()
                    .map(submission -> new PlagscanFilePayload(
                            submission.plagscanId(),
                            submission.codeBase64()
                    ))
                    .toList();

            PlagscanBaseCode baseCode = null;
            if (includeStarterCode && !StringUtils.isBlank(starterCode)) {
                baseCode = new PlagscanBaseCode(List.of(new PlagscanFilePayload(
                        STARTER_CODE_FILE_ID,
                        FileUtil.toBase64(starterCode)
                )));
            }

            return new PlagscanScanRequest(
                    payloads,
                    baseCode,
                    plagscanMinSimilarity,
                    true
            );
        }
    }

    public record TestCaseSnapshot(
            Long id,
            String input,
            String expectedOutput,
            TestVisibility visibility
    ) {
    }

    public record SubmissionSnapshot(
            Long id,
            String plagscanId,
            String code,
            String codeBase64
    ) {
    }
}
