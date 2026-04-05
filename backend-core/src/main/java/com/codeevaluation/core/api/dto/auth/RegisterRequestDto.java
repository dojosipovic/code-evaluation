package com.codeevaluation.core.api.dto.auth;

public record RegisterRequestDto(
        String token,
        String username,
        String firstname,
        String lastname,
        String password
) {
}
