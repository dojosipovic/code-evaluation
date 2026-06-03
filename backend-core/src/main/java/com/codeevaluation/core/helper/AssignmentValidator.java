package com.codeevaluation.core.helper;

import com.codeevaluation.core.api.dto.assignment.AssignmentCreateDto;
import com.codeevaluation.core.model.Assignment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;

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
                    "Assignment duration must be at least " + MIN_DURATION + " minutes"
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
}
