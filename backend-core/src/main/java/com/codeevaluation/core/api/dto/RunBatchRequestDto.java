package com.codeevaluation.core.api.dto;

import java.util.List;

public record RunBatchRequestDto(String code, Integer timeoutSec, List<TestCase> tests) {
}
