package com.codeevaluation.core.service.mail;

import io.quarkus.qute.Location;
import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class MailLayoutRenderer {

    @Location("mail/layout.html")
    private final Template layoutTemplate;
    @Location("mail/partials/header.html")
    private final Template headerTemplate;
    @Location("mail/partials/footer.html")
    private final Template footerTemplate;

    public String render(String title, String logoUrl, RawString bodyHtml) {
        String headerHtml = headerTemplate
                .data("brand", new MailBrandTemplateData(logoUrl))
                .render();
        String footerHtml = footerTemplate.render();

        return layoutTemplate
                .data("layout", new MailLayoutTemplateData(
                        title,
                        new RawString(headerHtml),
                        bodyHtml,
                        new RawString(footerHtml)
                ))
                .render();
    }
}
