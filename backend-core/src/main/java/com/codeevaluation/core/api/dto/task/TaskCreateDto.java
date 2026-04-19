package com.codeevaluation.core.api.dto.task;

import com.codeevaluation.core.enumeration.TaskStatus;
import java.util.List;

public record TaskCreateDto(
        String title,
        String description,
        StarterCodeDto starterCode,
        Boolean includeStarterCode,
        Boolean shared,
        Boolean enabled,
        TaskStatus status,
        List<TestDto> publicTests,
        List<TestDto> hiddenTests
) {
}
