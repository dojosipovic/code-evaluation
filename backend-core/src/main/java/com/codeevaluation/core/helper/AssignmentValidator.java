package com.codeevaluation.core.helper;

import com.codeevaluation.core.api.dto.assignment.AssignmentCreateDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentEvaluateRequestDto;
import com.codeevaluation.core.api.dto.submission.SubmissionGradeRequestDto;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Submission;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

@ApplicationScoped
public class AssignmentValidator {

    private static final Duration MIN_DURATION = Duration.ofMinutes(15);

    public void validateAssignment(AssignmentCreateDto assignmentCreateDto) {
        if (assignmentCreateDto == null) {
            throw new BadRequestException("Payload is required");
        }

        validateName(assignmentCreateDto.name());
        validateTaskId(assignmentCreateDto.taskId());
        validateStartsAt(assignmentCreateDto.startsAt());
        validateEndsAt(assignmentCreateDto);
        validatePoints(assignmentCreateDto.points());
    }

    private void validateName(String name) {
        String trimmedName = StringUtils.trimToEmpty(name);

        if (StringUtils.isBlank(trimmedName)) {
            throw new BadRequestException("Name is required");
        }

        if (trimmedName.length() > Assignment.NAME_MAX_LENGTH) {
            throw new BadRequestException(
                    "Name can have at most " + Assignment.NAME_MAX_LENGTH + " characters.");
        }
    }

    private void validateTaskId(Long taskId) {
        if (taskId == null) {
            throw new BadRequestException("Task is required");
        }

        if (taskId <= 0) {
            throw new BadRequestException("Task id must be positive");
        }
    }

    private void validateStartsAt(Instant startsAt) {
        if (startsAt == null) {
            throw new BadRequestException("Starts at is required");
        }

        if (!startsAt.isAfter(Instant.now())) {
            throw new BadRequestException("Starts at must be in the future");
        }
    }

    private void validateEndsAt(AssignmentCreateDto assignmentCreateDto) {
        if (assignmentCreateDto.endsAt() == null) {
            throw new BadRequestException("Ends at is required");
        }

        if (!assignmentCreateDto.endsAt().isAfter(assignmentCreateDto.startsAt())) {
            throw new BadRequestException("Ends at must be after starts at");
        }

        if (Duration.between(assignmentCreateDto.startsAt(), assignmentCreateDto.endsAt())
                .compareTo(MIN_DURATION) < 0) {
            throw new BadRequestException(
                    "Assignment duration must be at least " + MIN_DURATION.toMinutes() + " minutes"
            );
        }
    }

    private void validatePoints(Integer points) {
        if (points == null) {
            throw new BadRequestException("Points are required");
        }

        if (points <= 0) {
            throw new BadRequestException("Points must be positive");
        }
    }

    public void validateEvaluationRequest(
            AssignmentEvaluateRequestDto req,
            List<Submission> submissions,
            Integer assignmentPoints
    ) {
        List<SubmissionGradeRequestDto> requestedSubmissions =
                req.submissions();
        if (requestedSubmissions.size() != submissions.size()) {
            throw new BadRequestException("All assignment submissions must be evaluated");
        }

        Set<Long> requestedSubmissionIds =
                validateSubmissions(assignmentPoints, requestedSubmissions);

        Map<Long, Submission> submissionsById = submissions.stream()
                .collect(Collectors.toMap(Submission::getId, Function.identity()));
        if (!submissionsById.keySet().equals(requestedSubmissionIds)) {
            throw new BadRequestException("All and only assignment submissions must be evaluated");
        }

        boolean alreadyEvaluated = submissions.stream()
                .anyMatch(submission -> submission.getFinalScore() != null);
        if (alreadyEvaluated) {
            throw new WebApplicationException(
                    "Assignment contains already evaluated submissions",
                    Response.Status.CONFLICT
            );
        }
    }

    private static Set<Long> validateSubmissions(
            Integer assignmentPoints,
            List<SubmissionGradeRequestDto> requestedSubmissions
    ) {
        Set<Long> requestedSubmissionIds = new HashSet<>();
        for (SubmissionGradeRequestDto requestedSubmission
                : requestedSubmissions) {
            if (requestedSubmission == null || requestedSubmission.submissionId() == null) {
                throw new BadRequestException("Missing submission id");
            }
            if (requestedSubmission.finalGrade() == null) {
                throw new BadRequestException("Missing final grade");
            }
            if (requestedSubmission.finalGrade().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Final grade cannot be negative");
            }
            if (requestedSubmission.finalGrade()
                    .compareTo(BigDecimal.valueOf(assignmentPoints)) > 0) {
                throw new BadRequestException(
                        "Final grade cannot be greater than assignment points");
            }
            if (!requestedSubmissionIds.add(requestedSubmission.submissionId())) {
                throw new BadRequestException("Duplicate submission id");
            }
        }
        return requestedSubmissionIds;
    }
}
