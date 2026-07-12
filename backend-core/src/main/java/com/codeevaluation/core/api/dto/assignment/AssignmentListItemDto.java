package com.codeevaluation.core.api.dto.assignment;

import com.codeevaluation.core.api.dto.task.TaskBaseResponseDto;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.model.Assignment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record AssignmentListItemDto(
        Long id,
        Long submissionId,
        String name,
        Instant startsAt,
        Instant endsAt,
        Integer points,
        Boolean requiresEvaluation,
        TaskBaseResponseDto task,
        UserDto createdBy
) {

    public static AssignmentListItemDto from(
            Assignment assignment,
            boolean showTask,
            Long submissionId,
            Boolean requiresEvaluation
    ) {
        return AssignmentListItemDto.builder()
                .id(assignment.getId())
                .submissionId(submissionId)
                .name(assignment.getName())
                .startsAt(assignment.getStartsAt())
                .endsAt(assignment.getEndsAt())
                .points(assignment.getPoints())
                .requiresEvaluation(requiresEvaluation)
                .task(showTask ? TaskBaseResponseDto.from(assignment.getTask()) : null)
                .createdBy(UserDto.from(assignment.getCreatedBy()))
                .build();
    }

    public static List<AssignmentListItemDto> from(
            List<Assignment> assignments,
            boolean showTask,
            Map<Long, Long> submissionIdsByAssignmentId,
            Map<Long, Boolean> requiresEvaluationByAssignmentId
    ) {
        return assignments.stream()
                .map(a -> from(
                        a,
                        showTask,
                        submissionIdsByAssignmentId.get(a.getId()),
                        requiresEvaluationByAssignmentId.getOrDefault(a.getId(), false)
                ))
                .toList();
    }
}
