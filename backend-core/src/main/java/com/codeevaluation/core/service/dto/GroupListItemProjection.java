package com.codeevaluation.core.service.dto;

import com.codeevaluation.core.model.User;
import java.time.Instant;

public record GroupListItemProjection(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Long memberCount,
        User owner
) {
}
