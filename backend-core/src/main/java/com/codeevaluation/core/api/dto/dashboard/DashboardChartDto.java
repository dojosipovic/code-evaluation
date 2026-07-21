package com.codeevaluation.core.api.dto.dashboard;

import java.util.List;

public record DashboardChartDto(
        String key,
        String title,
        String type,
        List<String> labels,
        List<Number> values
) {
}
