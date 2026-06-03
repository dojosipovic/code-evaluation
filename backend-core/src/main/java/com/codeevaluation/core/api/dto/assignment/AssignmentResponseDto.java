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
        return AssignmentResponseDto.builder()
                .id(assignment.getId())
                .name(assignment.getName())
                .startsAt(assignment.getStartsAt())
                .endsAt(assignment.getEndsAt())
                .points(assignment.getPoints())
                .task(TaskResponseDto.from(assignment.getTask()))
                .createdBy(UserDto.from(assignment.getCreatedBy()))
                .build();
    }
}
