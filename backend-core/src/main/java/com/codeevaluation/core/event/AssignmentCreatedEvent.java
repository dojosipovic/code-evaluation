package com.codeevaluation.core.event;

import java.time.Instant;

public record AssignmentCreatedEvent(
        Long assignmentId,
        Instant startsAt
) {
}
