package com.codeevaluation.core.client.plagscan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlagscanFilePayload(
        @NotBlank
        String id,
        @NotBlank
        @NotNull
        String contentBase64
) {
}
