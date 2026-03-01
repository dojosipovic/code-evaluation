package com.codeevaluation.core.api.dto;

public record RunRequestDto(String code, String input, int timeoutSec) {
}
