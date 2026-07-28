package com.codeevaluation.core.service.mail;

import io.quarkus.qute.Location;
import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AssignmentPostActionMailTemplateRenderer {

    private final MailLayoutRenderer mailLayoutRenderer;
    @Location("mail/assignment-post-action/body.html")
    private final Template bodyTemplate;
    @Location("mail/assignment-post-action/text.txt")
    private final Template textTemplate;

    public MailContent render(AssignmentPostActionMailTemplateData templateData) {
        String bodyHtml = bodyTemplate.data("mail", templateData).render();

        return new MailContent(
                mailLayoutRenderer.render(
                        templateData.title(),
                        templateData.logoUrl(),
                        new RawString(bodyHtml)
                ),
                textTemplate.data("mail", templateData).render()
        );
    }
}
