package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.assignment.AssignmentCreateDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentResponseDto;
import com.codeevaluation.core.helper.AssignmentValidator;
import com.codeevaluation.core.helper.GroupAccessPolicy;
import com.codeevaluation.core.helper.TaskAccessPolicy;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.AssignmentRepository;
import com.codeevaluation.core.repository.GroupRepository;
import com.codeevaluation.core.repository.TaskRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final GroupRepository groupRepository;
    private final TaskRepository taskRepository;

    private final AssignmentValidator assignmentValidator;
    private final CurrentUserProvider currentUserProvider;

    private final GroupAccessPolicy groupAccessPolicy;
    private final TaskAccessPolicy taskAccessPolicy;

    public AssignmentResponseDto create(Long groupId, AssignmentCreateDto assignmentCreateDto) {
        assignmentValidator.validateAssignment(assignmentCreateDto);

        Group group = groupRepository.findByIdOptional(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
        Task task = taskRepository.findByIdOptional(assignmentCreateDto.taskId())
                .orElseThrow(() -> new NotFoundException("Task not found."));
        User currentUser = currentUserProvider.getCurrentUser();

        if (!groupAccessPolicy.canCreateAssignment(group, currentUser)) {
            throw new ForbiddenException("You don't have permission for this group");
        }

        if (!taskAccessPolicy.canUseTask(task, currentUser)) {
            throw new ForbiddenException("You don't have permission to use this task");
        }

        return AssignmentResponseDto.from(
                assignmentRepository.create(assignmentCreateDto, group, task, currentUser)
        );
    }

}
