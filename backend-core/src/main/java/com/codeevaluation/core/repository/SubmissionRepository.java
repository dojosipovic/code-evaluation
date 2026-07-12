package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.assignment.AssignmentSubmitRequestDto;
import com.codeevaluation.core.api.dto.submission.SubmissionFilterParams;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.enumeration.ProgrammingLanguage;
import com.codeevaluation.core.enumeration.SubmissionStatus;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Submission;
import com.codeevaluation.core.model.SubmissionFile;
import com.codeevaluation.core.model.SubmissionSimilarity;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class SubmissionRepository implements PanacheRepository<Submission> {

    private static final String DEFAULT_FILE_PATH = "src/main.cpp";

    public Optional<Submission> findByUserIdAndAssignmentId(Long userId, Long assignmentId) {
        return find(
                "user.id = ?1 and assignment.id = ?2",
                userId,
                assignmentId
        ).firstResultOptional();
    }

    public Map<Long, Long> findSubmissionIdsByUserIdAndAssignmentIds(
            Long userId,
            List<Long> assignmentIds
    ) {
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = getEntityManager()
                .createQuery(
                        """
                        select s.assignment.id, s.id
                        from Submission s
                        where s.user.id = :userId
                        and s.assignment.id in :assignmentIds
                        """,
                        Object[].class
                )
                .setParameter("userId", userId)
                .setParameter("assignmentIds", assignmentIds)
                .getResultList();

        return rows.stream().collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    public Map<Long, Boolean> findRequiresEvaluationByAssignmentIds(List<Long> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }

        List<Long> assignmentIdsRequiringEvaluation = getEntityManager()
                .createQuery(
                        """
                        select distinct s.assignment.id
                        from Submission s
                        where s.assignment.id in :assignmentIds
                        and s.finalScore is null
                        """,
                        Long.class
                )
                .setParameter("assignmentIds", assignmentIds)
                .getResultList();

        return assignmentIdsRequiringEvaluation.stream()
                .collect(Collectors.toMap(
                        assignmentId -> assignmentId,
                        assignmentId -> true
                ));
    }

    public Optional<Submission> findByUserIdAndAssignmentIdWithRelations(
            Long userId, Long assignmentId
    ) {
        return find(
                """
                select distinct s from Submission s
                join fetch s.user
                join fetch s.assignment
                join fetch s.task
                left join fetch s.files
                where s.user.id = ?1 and s.assignment.id = ?2
                """,
                userId,
                assignmentId
        ).stream().findFirst();
    }

    public Optional<Submission> findByIdWithRelations(Long submissionId) {
        return find(
                """
                select distinct s from Submission s
                join fetch s.user
                join fetch s.assignment a
                join fetch s.task
                join fetch a.group
                left join fetch s.files
                where s.id = ?1
                """,
                submissionId
        ).stream().findFirst();
    }

    public List<SubmissionSimilarity> findSimilaritiesForSubmission(Long submissionId) {
        return getEntityManager()
                .createQuery(
                        """
                        select distinct similarity from SubmissionSimilarity similarity
                        join fetch similarity.plagiarismRun plagiarismRun
                        join fetch similarity.sourceSubmission sourceSubmission
                        join fetch sourceSubmission.user sourceUser
                        join fetch similarity.targetSubmission targetSubmission
                        join fetch targetSubmission.user targetUser
                        where sourceSubmission.id = :submissionId
                        order by plagiarismRun.createdAt desc, similarity.similarityScore desc
                        """,
                        SubmissionSimilarity.class
                )
                .setParameter("submissionId", submissionId)
                .getResultList();
    }

    public List<Submission> findByAssignmentIdWithFiles(Long assignmentId) {
        return find(
                """
                select distinct s from Submission s
                join fetch s.assignment a
                join fetch s.task t
                join fetch s.user u
                left join fetch s.files
                where a.id = ?1
                order by s.id asc
                """,
                assignmentId
        ).list();
    }

    @Transactional
    public Submission createOrUpdate(
            AssignmentSubmitRequestDto req,
            Assignment assignment,
            User user,
            String codeBase64
    ) {
        return findByUserIdAndAssignmentId(user.getId(), assignment.getId())
                .map(submission -> update(submission, assignment, codeBase64, req.code()))
                .orElseGet(() -> create(req, assignment, user, codeBase64));
    }

    @Transactional
    public Submission create(
            AssignmentSubmitRequestDto req,
            Assignment assignment,
            User user,
            String codeBase64
    ) {
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setTask(assignment.getTask());
        submission.setUser(user);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setLanguage(ProgrammingLanguage.CPP);
        submission.addFile(buildFile(req.code(), codeBase64));

        persist(submission);

        return submission;
    }

    @Transactional
    public Submission update(
            Submission submission,
            Assignment assignment,
            String codeBase64,
            String code
    ) {
        submission.setAssignment(assignment);
        submission.setTask(assignment.getTask());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setLanguage(ProgrammingLanguage.CPP);
        submission.setFinalScore(null);

        SubmissionFile file = submission.getFiles().stream()
                .filter(existingFile -> DEFAULT_FILE_PATH.equals(existingFile.getFilePath()))
                .findFirst()
                .orElseGet(() -> {
                    SubmissionFile newFile = new SubmissionFile();
                    newFile.setFilePath(DEFAULT_FILE_PATH);
                    submission.addFile(newFile);
                    return newFile;
                });

        file.setContent(code);
        file.setContentBase64(codeBase64);

        return submission;
    }

    public PanacheQuery<Submission> findSubmissions(
            PagedContext pagedContext, SubmissionFilterParams submissionFilterParams) {

        StringBuilder query = new StringBuilder(
                """
                from Submission s
                join fetch s.user u
                join fetch s.assignment a
                join fetch s.task t
                join a.group g
                where 1=1
                """
        );
        Map<String, Object> params = new HashMap<>();
        User currentUser = submissionFilterParams.user();

        if (!StringUtils.isBlank(pagedContext.search())) {
            query.append(
                    """
                    and (
                            lower(a.name) like :search
                            or lower(u.username) like :search
                            or lower(u.firstname) like :search
                            or lower(u.lastname) like :search
                            or lower(concat(u.firstname, ' ', u.lastname)) like :search
                        )
                    """);

            params.put("search", "%" + pagedContext.search().toLowerCase().trim() + "%");
        }

        if (currentUser.isAdmin()) {
            appendUserFilter(query, params, submissionFilterParams.userId(), "requestedUserId");
        } else if (currentUser.getRole() == Role.PROF) {
            query.append(" and g.owner.id = :ownerId");
            params.put("ownerId", currentUser.getId());

            appendUserFilter(query, params, submissionFilterParams.userId(), "requestedUserId");
        } else {
            appendUserFilter(query, params, currentUser.getId(), "currentUserId");
            appendUserFilter(query, params, submissionFilterParams.userId(), "requestedUserId");
        }

        if (submissionFilterParams.assignmentId() != null) {
            query.append(" and a.id = :assignmentId");
            params.put("assignmentId", submissionFilterParams.assignmentId());
        }

        if (submissionFilterParams.status() != null) {
            query.append(" and s.status = :status");
            params.put("status", submissionFilterParams.status());
        }

        if (submissionFilterParams.submittedAfter() != null) {
            query.append(" and s.submittedAt >= :submittedAfter");
            params.put("submittedAfter", submissionFilterParams.submittedAfter());
        }

        if (submissionFilterParams.submittedBefore() != null) {
            query.append(" and s.submittedAt <= :submittedBefore");
            params.put("submittedBefore", submissionFilterParams.submittedBefore());
        }

        Sort sort = pagedContext.sort();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return find(query.toString(), sort, params).page(Page.of(page, size));
    }

    private void appendUserFilter(
            StringBuilder query,
            Map<String, Object> params,
            Long userId,
            String parameterName
    ) {
        if (userId == null) {
            return;
        }

        query.append(" and u.id = :").append(parameterName);
        params.put(parameterName, userId);
    }

    private SubmissionFile buildFile(String code, String codeBase64) {
        SubmissionFile file = new SubmissionFile();
        file.setFilePath(DEFAULT_FILE_PATH);
        file.setContent(code);
        file.setContentBase64(codeBase64);
        return file;
    }
}
