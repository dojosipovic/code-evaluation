package com.codeevaluation.core.api.dto.auth;

public record TwoFactorTotpVerifyRequestDto(String twoFactorToken, String code) {
}
