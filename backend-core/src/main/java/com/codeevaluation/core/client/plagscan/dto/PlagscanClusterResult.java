package com.codeevaluation.core.client.plagscan.dto;

import java.util.List;

public record PlagscanClusterResult(
        int clusterId,
        double similarity,
        List<String> members
) {
}
