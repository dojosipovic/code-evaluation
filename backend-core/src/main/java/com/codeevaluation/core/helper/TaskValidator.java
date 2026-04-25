package com.codeevaluation.core.helper;

import com.codeevaluation.core.api.dto.task.StarterCodeDto;
import com.codeevaluation.core.api.dto.task.TaskCreateDto;
import com.codeevaluation.core.api.dto.task.TaskPatchDto;
import com.codeevaluation.core.api.dto.task.TaskUpdateDto;
import com.codeevaluation.core.api.dto.task.TestDto;
import com.codeevaluation.core.enumeration.TestVisibility;
import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.TaskTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import java.util.List;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class TaskValidator {

    private static final int MIN_PUBLIC_TESTS = 3;
    private static final int MIN_HIDDEN_TESTS = 3;

    public void validateTask(TaskUpdateDto taskUpdateDto) {
        if (taskUpdateDto == null) {
            throw new BadRequestException("Payload is required");
        }

        validateTitle(taskUpdateDto.title());
        validateDescription(taskUpdateDto.description());
        validateStarterCode(taskUpdateDto.includeStarterCode(), taskUpdateDto.starterCode());
        validateTests(taskUpdateDto.publicTests(), TestVisibility.PUBLIC, MIN_PUBLIC_TESTS);
        validateTests(taskUpdateDto.hiddenTests(), TestVisibility.HIDDEN, MIN_HIDDEN_TESTS);
    }

    public void validateTask(TaskCreateDto taskCreateDto) {
        if (taskCreateDto == null) {
            throw new BadRequestException("Payload is required");
        }

        validateTitle(taskCreateDto.title());
        validateDescription(taskCreateDto.description());
        validateStarterCode(taskCreateDto.includeStarterCode(), taskCreateDto.starterCode());
        validateTests(taskCreateDto.publicTests(), TestVisibility.PUBLIC, MIN_PUBLIC_TESTS);
        validateTests(taskCreateDto.hiddenTests(), TestVisibility.HIDDEN, MIN_HIDDEN_TESTS);
    }

    public void validatePatch(TaskPatchDto taskPatchDto) {
        if (taskPatchDto == null) {
            throw new BadRequestException("Paylaod is required");
        }
    }

    private void validateTests(List<TestDto> publicTests, TestVisibility visibility, int minCount) {
        if (ListUtils.emptyIfNull(publicTests).size() < minCount) {
            throw new BadRequestException(
                    String.format(
                            "At least %s %s tests are required",
                            minCount,
                            visibility.name().toLowerCase()
                    )
            );
        }

        publicTests.forEach(this::validateSingleTest);
    }

    private void validateSingleTest(TestDto testDto) {
        if (testDto == null) {
            throw new BadRequestException("Test cannot be null");
        }

        String input = StringUtils.defaultIfEmpty(testDto.input(), "");
        String output = StringUtils.defaultIfEmpty(testDto.output(), "");

        if (input.length() > TaskTest.INPUT_MAX_LENGTH) {
            throw new BadRequestException(
                    "Input can have at most " + TaskTest.INPUT_MAX_LENGTH + " characters");
        }

        if (output.length() > TaskTest.INPUT_MAX_LENGTH) {
            throw new BadRequestException(
                    "Output can have at most " + TaskTest.OUTPUT_MAX_LENGTH + " characters");
        }
    }

    private void validateStarterCode(Boolean includeStarterCode, StarterCodeDto starterCodeDto) {
        includeStarterCode = BooleanUtils.isTrue(includeStarterCode);

        if (!includeStarterCode) {
            return;
        }

        if (starterCodeDto == null) {
            throw new BadRequestException(
                    "Starter code must be provided when includeStarterCode is true");
        }

        if (StringUtils.isBlank(starterCodeDto.code())) {
            throw new BadRequestException("Starter code code is required");
        }
    }

    private void validateDescription(String description) {
        if (StringUtils.isBlank(description)) {
            throw new BadRequestException("Description is required");
        }
    }

    private void validateTitle(String title) {
        title = StringUtils.trimToEmpty(title);

        if (StringUtils.isBlank(title)) {
            throw new BadRequestException("Title is required");
        }

        if (title.length() > Task.TITLE_MAX_LENGTH) {
            throw new BadRequestException(
                    "Title can have at most " + Task.TITLE_MAX_LENGTH + " characters.");
        }
    }
}
