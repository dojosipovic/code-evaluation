package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.assignment.AssignmentRunRequestDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentRunResponseDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentRunTestResultDto;
import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.assignment.AssignmentCreateDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentListItemDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentResponseDto;
import com.codeevaluation.core.api.dto.run.RunBatchResponseDto;
import com.codeevaluation.core.api.dto.run.TestCase;
import com.codeevaluation.core.api.dto.run.TestRunResult;
import com.codeevaluation.core.api.dto.run.RunRequestDto;
import com.codeevaluation.core.enumeration.TestResult;
import com.codeevaluation.core.helper.AssignmentAccessPolicy;
import com.codeevaluation.core.helper.AssignmentValidator;
import com.codeevaluation.core.helper.GroupAccessPolicy;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.helper.PagedParams;
import com.codeevaluation.core.helper.PagedSearchAssignmentImpl;
import com.codeevaluation.core.helper.TaskAccessPolicy;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.TaskTest;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.AssignmentRepository;
import com.codeevaluation.core.repository.GroupRepository;
import com.codeevaluation.core.repository.TaskRepository;
import com.codeevaluation.core.service.dto.RunResult;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

@ApplicationScoped
@RequiredArgsConstructor
public class AssignmentService {
    private static final List<String> RUNTIME_ERROR_STDERR_MARKERS = List.of(
            "fork failed",
            "resource temporarily unavailable",
            "cannot allocate memory",
            "out of memory",
            "pids-limit"
    );

    private final AssignmentRepository assignmentRepository;
    private final GroupRepository groupRepository;
    private final TaskRepository taskRepository;
    private final PagedSearchAssignmentImpl pagedSearchAssignment;
    private final CodeExecutionService codeExecutionService;

    private final AssignmentValidator assignmentValidator;
    private final CurrentUserProvider currentUserProvider;

    private final GroupAccessPolicy groupAccessPolicy;
    private final TaskAccessPolicy taskAccessPolicy;

    @Transactional
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

    public PagedResponse<AssignmentListItemDto> getGroupAssignments(
            Long groupId,
            PagedParams pagedParams
    ) {
        Group group = groupRepository.findByIdOptional(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        User currentUser = currentUserProvider.getCurrentUser();
        if (!groupAccessPolicy.canFetchAssignments(group, currentUser)) {
            throw new ForbiddenException("You cannot see assignments for this group");
        }

        boolean showTasks = groupAccessPolicy.canSeeAssignmentsTask(group, currentUser);
        PagedContext pagedContext = pagedSearchAssignment.generateFrom(pagedParams);
        PanacheQuery<Assignment> query =
                assignmentRepository.getGroupAssignments(groupId, pagedContext);

        List<AssignmentListItemDto> items = AssignmentListItemDto.from(query.list(), showTasks);
        long totalItems = query.count();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return new PagedResponse<>(items, page, size, totalItems);
    }

    public AssignmentRunResponseDto runAssignment(Long assignmentId, AssignmentRunRequestDto req) {
        if (req == null || StringUtils.isBlank(req.code())) {
            throw new BadRequestException("Missing code");
        }

        Assignment assignment = assignmentRepository.findByIdWithTaskAndTests(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        User currentUser = currentUserProvider.getCurrentUser();
        Group group = assignment.getGroup();

        if (!groupAccessPolicy.canFetchAssignments(group, currentUser)) {
            throw new ForbiddenException("You cannot run this assignment");
        }
        if (!assignment.isActive(Instant.now())) {
            throw new WebApplicationException("Assignment has expired",
                    Response.Status.CONFLICT);
        }

        List<TaskTest> tests = assignment.getTask().getTests();
        List<TestCase> inputs = TestCase.from(tests);
        var runBatchResponse = codeExecutionService.runBatch(req.code(), inputs);
        boolean showTestExpectedOutput =
                AssignmentAccessPolicy.showTestExpectedOutput(group, currentUser);

        return AssignmentRunResponseDto.from(assignment, runBatchResponse, showTestExpectedOutput);
    }
}
