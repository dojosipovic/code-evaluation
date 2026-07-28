package com.codeevaluation.core.service.mail;

public record MailBrandTemplateData(String logoUrl) {

    public boolean hasLogo() {
        return logoUrl != null && !logoUrl.isBlank();
    }
}
