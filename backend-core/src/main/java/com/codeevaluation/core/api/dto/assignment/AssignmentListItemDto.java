package com.codeevaluation.core.api.dto.assignment;

import com.codeevaluation.core.api.dto.task.TaskResponseDto;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Submission;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record AssignmentListItemDto(
        Long id,
        Long submissionId,
        String name,
        Instant startsAt,
        Instant endsAt,
        Integer points,
        TaskResponseDto task,
        UserDto createdBy
) {

    public static AssignmentListItemDto from(
            Assignment assignment,
            boolean showTask,
            Long currentUserId
    ) {
        return AssignmentListItemDto.builder()
                .id(assignment.getId())
                .submissionId(
                        assignment.getSubmissions().stream()
                                .filter(submission ->
                                        submission.getUser().getId().equals(currentUserId)
                                )
                                .map(Submission::getId)
                                .findFirst()
                                .orElse(null)
                )
                .name(assignment.getName())
                .startsAt(assignment.getStartsAt())
                .endsAt(assignment.getEndsAt())
                .points(assignment.getPoints())
                .task(showTask ? TaskResponseDto.from(assignment.getTask()) : null)
                .createdBy(UserDto.from(assignment.getCreatedBy()))
                .build();
    }

    public static List<AssignmentListItemDto> from(
            List<Assignment> assignments,
            boolean showTask,
            Long currentUserId
    ) {
        return assignments.stream().map(a -> from(a, showTask, currentUserId)).toList();
    }
}
