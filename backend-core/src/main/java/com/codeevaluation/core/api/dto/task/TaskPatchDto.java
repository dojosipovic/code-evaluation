package com.codeevaluation.core.api.dto.task;

public record TaskPatchDto(
        Boolean enabled,
        Boolean shared
) {
}
