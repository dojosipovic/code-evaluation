package com.codeevaluation.core.api.dto.submission;

import com.codeevaluation.core.enumeration.SubmissionTestRunStatus;
import com.codeevaluation.core.model.SubmissionTestRun;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record SubmissionTestRunDto(
        Long id,
        SubmissionTestRunStatus status,
        Integer totalTests,
        Integer passedTests,
        Long runtimeMs,
        Long memoryBytes,
        String logOutput,
        Instant createdAt,
        List<SubmissionTestResultDto> testResults
) {
    public static SubmissionTestRunDto from(
            SubmissionTestRun testRun,
            boolean showHiddenExpectedOutputs
    ) {
        return SubmissionTestRunDto.builder()
                .id(testRun.getId())
                .status(testRun.getStatus())
                .totalTests(testRun.getTotalTests())
                .passedTests(testRun.getPassedTests())
                .runtimeMs(testRun.getRuntimeMs())
                .memoryBytes(testRun.getMemoryBytes())
                .logOutput(testRun.getLogOutput())
                .createdAt(testRun.getCreatedAt())
                .testResults(SubmissionTestResultDto.from(
                        testRun.getTestResults(),
                        showHiddenExpectedOutputs
                ))
                .build();
    }

    public static List<SubmissionTestRunDto> from(
            List<SubmissionTestRun> testRuns,
            boolean showHiddenExpectedOutputs
    ) {
        return testRuns.stream()
                .map(testRun -> from(testRun, showHiddenExpectedOutputs))
                .toList();
    }
}
