package com.codeevaluation.core.api.dto.auth;

import java.util.List;

public record TwoFactorSettingsDto(
        boolean totpEnabled,
        boolean webauthnEnabled,
        List<WebAuthnCredentialDto> webauthnCredentials
) {
}
