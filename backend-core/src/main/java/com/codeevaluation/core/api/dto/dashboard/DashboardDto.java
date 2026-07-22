package com.codeevaluation.core.api.dto.dashboard;

import com.codeevaluation.core.enumeration.Role;
import java.util.List;

public record DashboardDto(
        Role role,
        List<DashboardStatDto> stats,
        List<DashboardChartDto> charts
) {
}
