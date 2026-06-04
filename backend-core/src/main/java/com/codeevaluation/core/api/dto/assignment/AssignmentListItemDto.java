package com.codeevaluation.core.api.dto.assignment;

import com.codeevaluation.core.api.dto.task.TaskResponseDto;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.model.Assignment;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record AssignmentListItemDto(
        Long id,
        String name,
        Instant startsAt,
        Instant endsAt,
        Integer points,
        TaskResponseDto task,
        UserDto createdBy
) {

    public static AssignmentListItemDto from(Assignment assignment, boolean showTask) {
        return AssignmentListItemDto.builder()
                .id(assignment.getId())
                .name(assignment.getName())
                .startsAt(assignment.getStartsAt())
                .endsAt(assignment.getEndsAt())
                .points(assignment.getPoints())
                .task(showTask ? TaskResponseDto.from(assignment.getTask()) : null)
                .createdBy(UserDto.from(assignment.getCreatedBy()))
                .build();
    }

    public static List<AssignmentListItemDto> from(List<Assignment> assignments, boolean showTask) {
        return assignments.stream().map(a -> from(a, showTask)).toList();
    }
}
