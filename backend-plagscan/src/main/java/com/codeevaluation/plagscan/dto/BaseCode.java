package com.codeevaluation.plagscan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BaseCode(
        @NotNull
        @NotEmpty
        List<@Valid FilePayload> files) {
}
