package com.codeevaluation.core.api.dto.task;

import java.util.List;

public record TaskCreateDto(
        String title,
        String description,
        StarterCodeDto starterCode,
        Boolean includeStarterCode,
        Boolean shared,
        Boolean enabled,
        List<TestDto> publicTests,
        List<TestDto> hiddenTests
) {
}
