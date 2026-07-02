package com.codeevaluation.plagscan.dto;

import io.smallrye.common.constraint.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import java.util.List;

@Getter
public class ScanRequest {
    @NotNull
    @NotEmpty
    private List<@Valid FilePayload> submissions;

    @Valid
    private BaseCode baseCode;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double minSimilarity;

    private boolean includeClusters = true;
}
