package com.codeevaluation.core.api.dto.assignment;

import com.codeevaluation.core.api.dto.run.TestRunResult;
import com.codeevaluation.core.enumeration.TestResult;
import com.codeevaluation.core.enumeration.TestVisibility;
import com.codeevaluation.core.helper.TestResultResolver;
import com.codeevaluation.core.model.TaskTest;
import lombok.Builder;

@Builder(toBuilder = true)
public record AssignmentRunTestResultDto(
        int index,
        String input,
        String expectedOutput,
        TestVisibility visibility,
        int exitCode,
        long durationMs,
        String stdout,
        String stderr,
        boolean timedOut,
        String timeout,
        TestResult testResult
) {

    public static AssignmentRunTestResultDto from(
            int index,
            TaskTest test,
            TestRunResult runResult
    ) {
        return AssignmentRunTestResultDto.builder()
                .index(index)
                .input(test.getInput())
                .expectedOutput(test.getOutput())
                .visibility(test.getVisibility())
                .exitCode(runResult.getExitCode())
                .durationMs(runResult.getDurationMs())
                .stdout(runResult.getStdout())
                .stderr(runResult.getStderr())
                .timedOut(runResult.isTimedOut())
                .timeout(runResult.getTimeout())
                .testResult(TestResultResolver.resolveFor(test.getOutput(), runResult))
                .build();
    }
}
