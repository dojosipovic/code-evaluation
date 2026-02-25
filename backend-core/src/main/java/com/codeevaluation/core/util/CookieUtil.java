package com.codeevaluation.core.util;

import jakarta.ws.rs.core.NewCookie;

public class CookieUtil {
    private CookieUtil() {}

    private static final int REFRESH_DAYS = 14;

    public static NewCookie buildRefreshCookie(String refreshPlain) {
        // IMPORTANT:
        // - secure(true) radi samo na HTTPS-u (u devu bez https stavi false)
        // - sameSite: LAX ako je frontend "isti site", NONE ako je druga domena (i onda secure mora biti true)
        return new NewCookie.Builder("refresh_token")
                .value(refreshPlain)
                .path("/auth")
                .httpOnly(true)
                .secure(false) // <-- u PRODUCTION: true (HTTPS)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(REFRESH_DAYS * 24 * 60 * 60)
                .build();
    }

    public static NewCookie deleteRefreshCookie() {
        return new NewCookie.Builder("refresh_token")
                .value("")
                .path("/auth")
                .httpOnly(true)
                .secure(false) // <-- u PRODUCTION: true
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(0)
                .build();
    }
}
