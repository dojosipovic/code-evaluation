package com.codeevaluation.core.api.dto.assignment;

import com.codeevaluation.core.api.dto.run.RunBatchResponseDto;
import com.codeevaluation.core.api.dto.run.TestRunResult;
import com.codeevaluation.core.enumeration.TestResult;
import com.codeevaluation.core.enumeration.TestVisibility;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.TaskTest;
import com.codeevaluation.core.service.dto.RunResult;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record AssignmentRunResponseDto(
        Long assignmentId,
        String assignmentName,
        RunResult compile,
        List<AssignmentRunTestResultDto> results,
        int passedCount,
        int totalCount
) {

    public static AssignmentRunResponseDto from(
            Assignment assignment,
            RunBatchResponseDto runBatchResponseDto,
            boolean showHiddenOutputs
    ) {
        RunResult compile = runBatchResponseDto.getCompile();
        if (compile.getExitCode() != 0 || compile.isTimedOut()) {
            return compileError(assignment, compile);
        }
        return getAssignmentRunResponseDto(runBatchResponseDto, assignment, showHiddenOutputs);
    }

    public static AssignmentRunResponseDto compileError(Assignment assignment, RunResult compile) {
        List<TaskTest> tests = assignment.getTask().getTests();
        return AssignmentRunResponseDto.builder()
                .assignmentId(assignment.getId())
                .assignmentName(assignment.getName())
                .compile(compile)
                .results(List.of())
                .passedCount(0)
                .totalCount(tests.size())
                .build();
    }

    private static boolean showTestOutput(TaskTest test, boolean showHiddenOutputs) {
        return Objects.equals(test.getVisibility(), TestVisibility.PUBLIC) || showHiddenOutputs;
    }

    private static @NonNull AssignmentRunResponseDto getAssignmentRunResponseDto(
            RunBatchResponseDto runBatchResponse, Assignment assignment,
            boolean showHiddenOutputs) {
        List<TaskTest> tests = assignment.getTask().getTests();
        List<AssignmentRunTestResultDto> results = new java.util.ArrayList<>();
        int passedCount = 0;

        for (int i = 0; i < tests.size(); i++) {
            TaskTest test = tests.get(i);
            TestRunResult runResult = runBatchResponse.getResults().get(i);
            boolean showOutput = showTestOutput(test, showHiddenOutputs);
            String expectedOutput = showOutput ? test.getOutput() : null;

            var result = AssignmentRunTestResultDto.from(i, test, runResult).toBuilder()
                    .expectedOutput(expectedOutput)
                    .showExpectedOutput(showOutput)
                    .build();

            if (Objects.equals(result.testResult(), TestResult.PASSED)) {
                passedCount++;
            }

            results.add(result);
        }

        return new AssignmentRunResponseDto(
                assignment.getId(),
                assignment.getName(),
                runBatchResponse.getCompile(),
                results,
                passedCount,
                results.size()
        );
    }
}
