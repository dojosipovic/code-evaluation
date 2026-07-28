package com.codeevaluation.core.service.mail;

import io.quarkus.qute.RawString;

public record MailLayoutTemplateData(
        String title,
        RawString header,
        RawString body,
        RawString footer
) {
}
