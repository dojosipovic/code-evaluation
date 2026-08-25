package com.codeevaluation.core.api.dto.auth;

public record WebAuthnFinishRequestDto(String token, String responseJson) {
}
