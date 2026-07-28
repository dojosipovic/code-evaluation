package com.codeevaluation.core.service.mail;

public record InviteMailTemplateData(
        String title,
        String inviterName,
        String email,
        String role,
        String expiresAt,
        String inviteUrl,
        String logoUrl
) {

    public boolean hasLogo() {
        return logoUrl != null && !logoUrl.isBlank();
    }
}
