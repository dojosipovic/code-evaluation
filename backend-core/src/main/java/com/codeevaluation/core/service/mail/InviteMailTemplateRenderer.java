package com.codeevaluation.core.service.mail;

import io.quarkus.qute.Location;
import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class InviteMailTemplateRenderer {

    @Location("mail/layout.html")
    private final Template layoutTemplate;
    @Location("mail/partials/header.html")
    private final Template headerTemplate;
    @Location("mail/partials/footer.html")
    private final Template footerTemplate;
    @Location("mail/invite/body.html")
    private final Template inviteBodyTemplate;
    @Location("mail/invite.txt")
    private final Template inviteTextTemplate;

    public InviteMailContent render(InviteMailTemplateData templateData) {
        String headerHtml = headerTemplate.data("mail", templateData).render();
        String bodyHtml = inviteBodyTemplate.data("mail", templateData).render();
        String footerHtml = footerTemplate.render();
        String html = layoutTemplate
                .data("layout", new MailLayoutTemplateData(
                        templateData.title(),
                        new RawString(headerHtml),
                        new RawString(bodyHtml),
                        new RawString(footerHtml)
                ))
                .render();

        return new InviteMailContent(
                html,
                inviteTextTemplate.data("mail", templateData).render()
        );
    }
}
