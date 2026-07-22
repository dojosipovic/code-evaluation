package com.codeevaluation.core.client.plagscan.dto;

public record PlagscanPairResult(
        String studentA,
        String studentB,
        double similarity
) {
}
