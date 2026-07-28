package com.codeevaluation.core.service.mail;

import io.quarkus.qute.Location;
import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class InviteMailTemplateRenderer {

    private final MailLayoutRenderer mailLayoutRenderer;
    @Location("mail/invite/body.html")
    private final Template inviteBodyTemplate;
    @Location("mail/invite/text.txt")
    private final Template inviteTextTemplate;

    public MailContent render(InviteMailTemplateData templateData) {
        String bodyHtml = inviteBodyTemplate.data("mail", templateData).render();

        return new MailContent(
                mailLayoutRenderer.render(
                        templateData.title(),
                        templateData.logoUrl(),
                        new RawString(bodyHtml)
                ),
                inviteTextTemplate.data("mail", templateData).render()
        );
    }
}
