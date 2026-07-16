package com.codeevaluation.core.api.dto.assignment;

import com.codeevaluation.core.model.User;
import lombok.Builder;

@Builder
public record AssignmentFilterParams(
        Long groupId,
        Long currentUserId,
        Boolean active,
        Boolean submitted,
        Boolean ungraded,
        User user
) {}
