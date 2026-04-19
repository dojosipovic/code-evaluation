package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.task.StarterCodeDto;
import com.codeevaluation.core.api.dto.task.TaskCreateDto;
import com.codeevaluation.core.api.dto.task.TaskResponseDto;
import com.codeevaluation.core.api.dto.task.TestDto;
import com.codeevaluation.core.enumeration.TaskStatus;
import com.codeevaluation.core.enumeration.TestVisibility;
import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.TaskTest;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.repository.TaskRepository;
import com.codeevaluation.core.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@RequiredArgsConstructor
public class TaskService {

    private static final int MIN_PUBLIC_TESTS = 3;
    private static final int MIN_HIDDEN_TESTS = 3;

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public TaskResponseDto createTask(TaskCreateDto taskCreateDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        validateTask(taskCreateDto);

        Task task = taskRepository.create(taskCreateDto, user);
        return TaskResponseDto.from(task);
    }

    public TaskResponseDto getTask(Long id, String username) {
        Task task = taskRepository.getTask(id)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        boolean isUserOwner = username.equals(task.getUser().getUsername());
        boolean isShared = Boolean.TRUE.equals(task.getShared());
        boolean isPublished = task.getStatus() == TaskStatus.PUBLISHED;

        if (isUserOwner || isShared && isPublished) {
            return TaskResponseDto.from(task);
        }

        if (!isShared) {
            throw new BadRequestException("Task is not shared");
        }

        throw new BadRequestException("Task is not in status " + TaskStatus.PUBLISHED);
    }

    private void validateTask(TaskCreateDto taskCreateDto) {
        if (taskCreateDto == null) {
            throw new BadRequestException("Payload is required");
        }

        validateTitle(taskCreateDto);
        validateDescription(taskCreateDto);
        validateStarterCode(taskCreateDto);
        validateTests(taskCreateDto.publicTests(), TestVisibility.PUBLIC, MIN_PUBLIC_TESTS);
        validateTests(taskCreateDto.hiddenTests(), TestVisibility.HIDDEN, MIN_HIDDEN_TESTS);
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

    private void validateStarterCode(TaskCreateDto taskCreateDto) {
        boolean includeStarterCode = BooleanUtils.isTrue(taskCreateDto.includeStarterCode());

        if (!includeStarterCode) {
            return;
        }

        StarterCodeDto starterCodeDto = taskCreateDto.starterCode();
        if (starterCodeDto == null) {
            throw new BadRequestException(
                    "Starter code must be provided when includeStarterCode is true");
        }

        if (StringUtils.isBlank(starterCodeDto.code())) {
            throw new BadRequestException("Starter code code is required");
        }
    }

    private void validateDescription(TaskCreateDto taskCreateDto) {
        if (StringUtils.isBlank(taskCreateDto.description())) {
            throw new BadRequestException("Description is required");
        }
    }

    private void validateTitle(TaskCreateDto taskCreateDto) {
        String title = StringUtils.trimToEmpty(taskCreateDto.title());

        if (StringUtils.isBlank(title)) {
            throw new BadRequestException("Title is required");
        }

        if (title.length() > Task.TITLE_MAX_LENGTH) {
            throw new BadRequestException(
                    "Title can have at most " + Task.TITLE_MAX_LENGTH + " characters.");
        }
    }
}
