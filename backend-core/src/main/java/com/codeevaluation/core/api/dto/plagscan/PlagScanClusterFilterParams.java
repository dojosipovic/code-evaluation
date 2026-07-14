package com.codeevaluation.core.api.dto.plagscan;

import com.codeevaluation.core.model.User;
import lombok.Builder;

@Builder
public record PlagScanClusterFilterParams(
        Long assignmentId,
        User user
) {}
