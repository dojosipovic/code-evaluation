package com.codeevaluation.core.event;

import java.time.Instant;

public record AssignmentCreateEvent(
        Long assignmentId,
        Instant startsAt,
        Instant endsAt
) {
}
