package com.codeevaluation.core.api.dto.dashboard;

public record DashboardStatDto(
        String key,
        String label,
        Number value,
        String suffix
) {
}
