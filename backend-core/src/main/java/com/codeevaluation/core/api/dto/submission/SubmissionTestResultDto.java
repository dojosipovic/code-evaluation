package com.codeevaluation.core.api.dto.submission;

import com.codeevaluation.core.enumeration.TestResult;
import com.codeevaluation.core.enumeration.TestVisibility;
import com.codeevaluation.core.model.SubmissionTestResult;
import java.util.List;
import java.util.Objects;
import lombok.Builder;

@Builder
public record SubmissionTestResultDto(
        Long id,
        Long taskTestId,
        TestResult result,
        TestVisibility visibility,
        String testInput,
        String expectedOutput,
        boolean showExpectedOutput,
        String actualOutput,
        Long runtimeMs,
        String errorOutput
) {
    public static SubmissionTestResultDto from(
            SubmissionTestResult testResult,
            boolean showHiddenExpectedOutputs
    ) {
        boolean showExpectedOutput = showExpectedOutput(
                testResult.getVisibility(),
                showHiddenExpectedOutputs
        );

        return SubmissionTestResultDto.builder()
                .id(testResult.getId())
                .taskTestId(testResult.getTaskTest() != null
                        ? testResult.getTaskTest().getId()
                        : null)
                .result(testResult.getResult())
                .visibility(testResult.getVisibility())
                .testInput(testResult.getTestInput())
                .expectedOutput(showExpectedOutput ? testResult.getExpectedOutput() : null)
                .showExpectedOutput(showExpectedOutput)
                .actualOutput(testResult.getActualOutput())
                .runtimeMs(testResult.getRuntimeMs())
                .errorOutput(testResult.getErrorOutput())
                .build();
    }

    public static List<SubmissionTestResultDto> from(
            List<SubmissionTestResult> testResults,
            boolean showHiddenExpectedOutputs
    ) {
        return testResults.stream()
                .map(testResult -> from(testResult, showHiddenExpectedOutputs))
                .toList();
    }

    private static boolean showExpectedOutput(
            TestVisibility visibility,
            boolean showHiddenExpectedOutputs
    ) {
        return Objects.equals(visibility, TestVisibility.PUBLIC) || showHiddenExpectedOutputs;
    }
}
