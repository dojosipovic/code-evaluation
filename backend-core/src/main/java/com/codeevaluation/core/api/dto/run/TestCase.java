package com.codeevaluation.core.api.dto.run;

import com.codeevaluation.core.model.TaskTest;
import java.util.List;

public record TestCase(String input) {

    public static TestCase from(TaskTest test) {
        return new TestCase(test.getInput());
    }

    public static List<TestCase> from(List<TaskTest> tests) {
        return tests.stream()
                .map(TestCase::from)
                .toList();
    }
}
