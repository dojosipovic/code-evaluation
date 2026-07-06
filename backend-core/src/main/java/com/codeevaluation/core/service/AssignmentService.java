package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.assignment.AssignmentRunRequestDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentRunResponseDto;
import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.assignment.AssignmentCreateDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentListItemDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentResponseDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentSubmitRequestDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentSubmitResponseDto;
import com.codeevaluation.core.api.dto.submission.SubmissionResponseDto;
import com.codeevaluation.core.api.dto.run.TestCase;
import com.codeevaluation.core.event.AssignmentCreateEvent;
import com.codeevaluation.core.helper.AssignmentAccessPolicy;
import com.codeevaluation.core.helper.AssignmentValidator;
import com.codeevaluation.core.helper.GroupAccessPolicy;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.api.query.PagedParams;
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
import com.codeevaluation.core.repository.SubmissionRepository;
import com.codeevaluation.core.repository.TaskRepository;
import com.codeevaluation.core.util.FileUtil;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final GroupRepository groupRepository;
    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final PagedSearchAssignmentImpl pagedSearchAssignment;
    private final CodeExecutionService codeExecutionService;
    private final Event<AssignmentCreateEvent> assignmentCreatedEvent;

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

        Assignment assignment =
                assignmentRepository.create(assignmentCreateDto, group, task, currentUser);
        assignmentCreatedEvent.fire(new AssignmentCreateEvent(
                assignment.getId(),
                assignment.getStartsAt(),
                assignment.getEndsAt()
        ));

        return AssignmentResponseDto.from(assignment);
    }

    public AssignmentResponseDto get(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithTaskAndTests(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        User currentUser = currentUserProvider.getCurrentUser();
        Group group = assignment.getGroup();
        if (!AssignmentAccessPolicy.canSeeAssignment(group, currentUser)) {
            throw new ForbiddenException("You cannot see this assignment");
        }

        boolean showTestExpectedOutput =
                AssignmentAccessPolicy.showTestExpectedOutput(group, currentUser);
        SubmissionResponseDto submission = submissionRepository
                .findByUserIdAndAssignmentIdWithRelations(currentUser.getId(), assignmentId)
                .map(SubmissionResponseDto::from)
                .orElse(null);

        return AssignmentResponseDto.from(assignment, submission, showTestExpectedOutput);
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
        List<Assignment> assignments = query.list();
        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        Map<Long, Long> submissionIdsByAssignmentId =
                submissionRepository.findSubmissionIdsByUserIdAndAssignmentIds(
                        currentUser.getId(),
                        assignmentIds
                );

        List<AssignmentListItemDto> items = AssignmentListItemDto.from(
                        assignments,
                        showTasks,
                        submissionIdsByAssignmentId
                );

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

    @Transactional
    public AssignmentSubmitResponseDto submitAssignment(
            Long assignmentId, AssignmentSubmitRequestDto req
    ) {
        if (req == null || StringUtils.isBlank(req.code())) {
            throw new BadRequestException("Missing code");
        }

        Assignment assignment = assignmentRepository.findByIdWithTaskAndTests(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        User currentUser = currentUserProvider.getCurrentUser();
        Group group = assignment.getGroup();

        if (!AssignmentAccessPolicy.canSubmitAssignment(group, currentUser)) {
            throw new ForbiddenException("You cannot submit this assignment");
        }
        if (!assignment.isActive(Instant.now())) {
            throw new WebApplicationException("Assignment has expired",
                    Response.Status.CONFLICT);
        }

        String codeBase64 = FileUtil.toBase64(req.code());
        return AssignmentSubmitResponseDto.from(
                submissionRepository.createOrUpdate(req, assignment, currentUser, codeBase64)
        );
    }
}
