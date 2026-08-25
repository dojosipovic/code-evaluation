package com.codeevaluation.core.api.dto.auth;

import com.codeevaluation.core.model.WebAuthnCredential;
import java.time.Instant;

public record WebAuthnCredentialDto(
        Long id,
        String credentialId,
        Boolean discoverable,
        Boolean backedUp,
        Instant createdAt,
        Instant lastUsedAt
) {

    public static WebAuthnCredentialDto from(WebAuthnCredential credential) {
        return new WebAuthnCredentialDto(
                credential.getId(),
                credential.getCredentialId(),
                credential.getDiscoverable(),
                credential.getBackedUp(),
                credential.getCreatedAt(),
                credential.getLastUsedAt()
        );
    }
}
