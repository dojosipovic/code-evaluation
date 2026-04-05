package com.codeevaluation.core.api.dto.run;

public record RunRequestDto(String code, String input, int timeoutSec) {
}
