package com.codeevaluation.core.helper;

import com.codeevaluation.core.api.dto.run.TestRunResult;
import com.codeevaluation.core.enumeration.TestResult;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class TestResultResolver {

    public static TestResult resolveFor(String expectedOutput, TestRunResult runResult) {
        if (runResult == null) {
            return TestResult.INTERNAL_ERROR;
        }
        if (runResult.isTimedOut()) {
            return TestResult.TIME_LIMIT_EXCEEDED;
        }
        if (isRuntimeError(runResult)) {
            return TestResult.RUNTIME_ERROR;
        }
        if (!normalizeOutput(expectedOutput).equals(normalizeOutput(runResult.getStdout()))) {
            return TestResult.WRONG_ANSWER;
        }
        return TestResult.PASSED;
    }

    private static String normalizeOutput(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").stripTrailing();
    }

    private static boolean isRuntimeError(TestRunResult runResult) {
        return runResult.getExitCode() != 0
                || !StringUtils.isBlank(runResult.getStderr());
    }
}
