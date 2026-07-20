package com.codeevaluation.core.client.plagscan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PlagscanScanRequest(
        @NotNull
        @NotEmpty
        List<@Valid PlagscanFilePayload> submissions,
        @Valid
        PlagscanBaseCode baseCode,
        @NotNull
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        Double minSimilarity,
        boolean includeClusters
) {
}
