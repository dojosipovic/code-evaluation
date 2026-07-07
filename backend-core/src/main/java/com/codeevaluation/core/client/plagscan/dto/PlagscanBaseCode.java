package com.codeevaluation.core.client.plagscan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PlagscanBaseCode(
        @NotNull
        @NotEmpty
        List<@Valid PlagscanFilePayload> files
) {
}
