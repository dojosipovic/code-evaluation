package com.codeevaluation.core.api.dto.auth;

import java.util.List;

public record LoginResponseDto(
        String status,
        String accessToken,
        String twoFactorToken,
        String primaryMethod,
        List<String> availableMethods
) {

    public static LoginResponseDto authenticated(String accessToken) {
        return new LoginResponseDto("AUTHENTICATED", accessToken, null, null, List.of());
    }

    public static LoginResponseDto twoFactorRequired(
            String twoFactorToken,
            String primaryMethod,
            List<String> availableMethods
    ) {
        return new LoginResponseDto(
                "TWO_FACTOR_REQUIRED",
                null,
                twoFactorToken,
                primaryMethod,
                availableMethods
        );
    }
}
