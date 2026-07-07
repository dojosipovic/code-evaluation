package com.codeevaluation.core.client.plagscan.dto;

import java.util.List;

public record PlagscanResult(
        String runId,
        Double minSimilarity,
        List<PlagscanPairResult> pairs,
        List<PlagscanClusterResult> clusters,
        String fileBase64
) {
}
