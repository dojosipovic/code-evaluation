package com.codeevaluation.core.api.dto.run;

import java.util.List;

public record RunBatchRequestDto(String code, List<TestCase> tests) {
}
