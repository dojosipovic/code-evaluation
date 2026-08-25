package com.codeevaluation.core.api.dto.auth;

public record TotpSetupResponseDto(String secret, String otpauthUrl) {
}
