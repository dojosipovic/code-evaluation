package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.task.TaskCreateDto;
import com.codeevaluation.core.api.dto.task.TaskPatchDto;
import com.codeevaluation.core.api.dto.task.TaskResponseDto;
import com.codeevaluation.core.api.dto.task.TaskUpdateDto;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.enumeration.TaskStatus;
import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.TaskRepository;
import com.codeevaluation.core.validator.TaskValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskValidator taskValidator;

    public TaskResponseDto createTask(TaskCreateDto taskCreateDto) {
        User currentUser = currentUserProvider.getCurrentUser();
        taskValidator.validateTask(taskCreateDto);

        Task task = taskRepository.create(taskCreateDto, currentUser);
        return TaskResponseDto.from(task);
    }

    @Transactional
    public TaskResponseDto publishTask(Long id) {
        Task task = taskRepository.getTask(id)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        if (!canModifyTask(task)) {
            throw new ForbiddenException("You cannot edit someone else task");
        }

        if (task.getStatus() == TaskStatus.PUBLISHED) {
            throw new BadRequestException("Task is already published");
        }

        return TaskResponseDto.from(taskRepository.publish(task));
    }

    @Transactional
    public TaskResponseDto patchTask(TaskPatchDto taskPatchDto, Long id) {
        taskValidator.validatePatch(taskPatchDto);

        Task task = taskRepository.getTask(id)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        if (!canModifyTask(task)) {
            throw new ForbiddenException("You cannot modify someone else task");
        }

        if (task.getStatus() != TaskStatus.PUBLISHED) {
            throw new BadRequestException("Task must be in status " + TaskStatus.PUBLISHED + " to be patched");
        }

        return TaskResponseDto.from(taskRepository.patch(taskPatchDto, task));
    }

    @Transactional
    public TaskResponseDto updateTask(TaskUpdateDto taskUpdateDto, Long id) {
        Task task = taskRepository.getTask(id)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        if (task.getStatus() == TaskStatus.PUBLISHED) {
            throw new BadRequestException("Task in status " + TaskStatus.PUBLISHED + " cannot be edited");
        }

        taskValidator.validateTask(taskUpdateDto);

        if (!canModifyTask(task)) {
            throw new ForbiddenException("You cannot edit someone else task");
        }

        return TaskResponseDto.from(taskRepository.update(task, taskUpdateDto));
    }

    public TaskResponseDto getTask(Long id) {
        Task task = taskRepository.getTask(id)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        boolean isShared = Boolean.TRUE.equals(task.getShared());
        boolean isPublished = task.getStatus() == TaskStatus.PUBLISHED;

        if (canModifyTask(task) || isShared && isPublished) {
            return TaskResponseDto.from(task);
        }

        if (!isShared) {
            throw new ForbiddenException("Task is not shared");
        }

        throw new BadRequestException("Task is not in status " + TaskStatus.PUBLISHED);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.getTask(id)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        if (task.getStatus() == TaskStatus.PUBLISHED) {
            throw new BadRequestException("Task in status " + TaskStatus.PUBLISHED + " cannot be deleted");
        }

        if (!canModifyTask(task)) {
            throw new ForbiddenException("You cannot delete someone else task");
        }

        taskRepository.delete(task);
    }

    private boolean canModifyTask(Task task) {
        User currentUser = currentUserProvider.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isUserOwner = currentUser.getUsername().equals(task.getUser().getUsername());

        return isAdmin || isUserOwner;
    }
}
