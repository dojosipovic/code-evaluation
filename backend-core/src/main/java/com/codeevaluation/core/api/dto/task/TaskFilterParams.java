package com.codeevaluation.core.api.dto.task;

import com.codeevaluation.core.enumeration.TaskStatus;
import com.codeevaluation.core.model.User;
import lombok.Builder;

@Builder
public record TaskFilterParams(
        TaskStatus status,
        Boolean enabled,
        Boolean shared,
        User user,
        Boolean excludeUser
) {}
