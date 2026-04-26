package com.codeevaluation.core.service;

import com.codeevaluation.core.TaskListQueryParams;
import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.task.TaskCreateDto;
import com.codeevaluation.core.api.dto.task.TaskFilterParams;
import com.codeevaluation.core.api.dto.task.TaskListItemDto;
import com.codeevaluation.core.api.dto.task.TaskPatchDto;
import com.codeevaluation.core.api.dto.task.TaskResponseDto;
import com.codeevaluation.core.api.dto.task.TaskUpdateDto;
import com.codeevaluation.core.enumeration.TaskStatus;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.helper.PagedSearchTaskImpl;
import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.TaskRepository;
import com.codeevaluation.core.helper.TaskValidator;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TaskValidator taskValidator;
    private final PagedSearchTaskImpl pagedSearchTask;

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
            throw new BadRequestException(
                    "Task must be in status " + TaskStatus.PUBLISHED + " to be patched");
        }

        return TaskResponseDto.from(taskRepository.patch(taskPatchDto, task));
    }

    @Transactional
    public TaskResponseDto updateTask(TaskUpdateDto taskUpdateDto, Long id) {
        Task task = taskRepository.getTask(id)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        if (task.getStatus() == TaskStatus.PUBLISHED) {
            throw new BadRequestException(
                    "Task in status " + TaskStatus.PUBLISHED + " cannot be edited");
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
            throw new BadRequestException(
                    "Task in status " + TaskStatus.PUBLISHED + " cannot be deleted");
        }

        if (!canModifyTask(task)) {
            throw new ForbiddenException("You cannot delete someone else task");
        }

        taskRepository.delete(task);
    }

    private boolean canModifyTask(Task task) {
        User currentUser = currentUserProvider.getCurrentUser();
        boolean isUserOwner = currentUser.getUsername().equals(task.getUser().getUsername());

        return currentUser.isAdmin() || isUserOwner;
    }

    public PagedResponse<TaskListItemDto> getTasks(TaskListQueryParams taskListQueryParams) {
        User currentUser = currentUserProvider.getCurrentUser();
        PagedContext pagedContext = pagedSearchTask.generateFrom(taskListQueryParams);
        TaskFilterParams taskFilterParams =
                pagedSearchTask.generateFilterParams(taskListQueryParams)
                        .user(currentUser)
                        .build();

        PanacheQuery<Task> query = taskRepository.findTasks(pagedContext, taskFilterParams);
        List<TaskListItemDto> items = TaskListItemDto.from(query.list());
        long totalItems = query.count();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return new PagedResponse<>(items, page, size, totalItems);
    }
}
