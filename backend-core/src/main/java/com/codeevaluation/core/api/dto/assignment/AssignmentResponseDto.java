package com.codeevaluation.core.api.dto.assignment;

import com.codeevaluation.core.api.dto.task.TaskResponseDto;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.model.Assignment;
import java.time.Instant;
import lombok.Builder;

@Builder
public record AssignmentResponseDto(
        Long id,
        String name,
        Instant startsAt,
        Instant endsAt,
        Integer points,
        TaskResponseDto task,
        UserDto createdBy
) {

    public static AssignmentResponseDto from(Assignment assignment) {
        return buildGeneralInfo(assignment)
                .task(TaskResponseDto.from(assignment.getTask()))
                .build();
    }

    public static AssignmentResponseDto from(
            Assignment assignment,
            boolean showTestExpectedOutput
    ) {
        return buildGeneralInfo(assignment)
                .task(TaskResponseDto.from(assignment.getTask(), showTestExpectedOutput))
                .build();
    }

    private static AssignmentResponseDtoBuilder buildGeneralInfo(Assignment assignment) {
        return AssignmentResponseDto.builder()
                .id(assignment.getId())
                .name(assignment.getName())
                .startsAt(assignment.getStartsAt())
                .endsAt(assignment.getEndsAt())
                .points(assignment.getPoints())
                .createdBy(UserDto.from(assignment.getCreatedBy()));
    }
}
