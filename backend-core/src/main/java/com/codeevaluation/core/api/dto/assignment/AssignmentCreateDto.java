package com.codeevaluation.core.api.dto.assignment;

import java.time.Instant;

public record AssignmentCreateDto(
        String name,
        Long taskId,
        Long groupId,
        Instant startsAt,
        Instant endsAt,
        Integer points
) {
}
