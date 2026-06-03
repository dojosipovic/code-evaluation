package com.codeevaluation.core.api.dto.assignment;

import java.time.Instant;

public record AssignmentCreateDto(
        String name,
        Long taskId,
        Instant startsAt,
        Instant endsAt,
        Integer points
) {
}
