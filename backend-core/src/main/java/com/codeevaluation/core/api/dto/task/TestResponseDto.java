package com.codeevaluation.core.api.dto.task;

import com.codeevaluation.core.enumeration.TestVisibility;
import com.codeevaluation.core.model.TaskTest;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TestResponseDto {

    private String input;
    private String output;
    private TestVisibility visibility;

    public static TestResponseDto from(TaskTest taskTest) {
        TestResponseDto testResponseDto = new TestResponseDto();

        testResponseDto.setInput(taskTest.getInput());
        testResponseDto.setOutput(taskTest.getOutput());
        testResponseDto.setVisibility(taskTest.getVisibility());

        return testResponseDto;
    }

    public static TestResponseDto from(TaskTest taskTest, boolean showTestExpectedOutput) {
        String output = showOutput(taskTest, showTestExpectedOutput)
                ? taskTest.getOutput() : null;
        TestResponseDto testResponseDto = new TestResponseDto();

        testResponseDto.setInput(taskTest.getInput());
        testResponseDto.setOutput(output);
        testResponseDto.setVisibility(taskTest.getVisibility());

        return testResponseDto;
    }

    public static List<TestResponseDto> from(List<TaskTest> taskTests) {
        return taskTests.stream().map(TestResponseDto::from).toList();
    }

    public static List<TestResponseDto> from(
            List<TaskTest> taskTests, boolean showTestExpectedOutput
    ) {
        return taskTests.stream().map(tt -> from(tt, showTestExpectedOutput)).toList();
    }

    private static boolean showOutput(TaskTest taskTest, boolean showTestExpectedOutput) {
        return Objects.equals(taskTest.getVisibility(), TestVisibility.PUBLIC)
                || showTestExpectedOutput;
    }
}
