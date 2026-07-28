package com.codeevaluation.core.service;

import com.codeevaluation.core.config.MailConfig;
import com.codeevaluation.core.config.MetadataConfig;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Invite;
import com.codeevaluation.core.service.mail.InviteMailContent;
import com.codeevaluation.core.service.mail.InviteMailTemplateData;
import com.codeevaluation.core.service.mail.InviteMailTemplateRenderer;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final MetadataConfig metadataConfig;
    private final MailConfig mailConfig;
    private final InviteMailTemplateRenderer inviteMailTemplateRenderer;
    private final ReactiveMailer mailer;

    public void sendTestMail(String to) {
        mailer.send(
                Mail.withText(
                        to,
                        "Test poruka iz Quarkusa",
                        "Ovo je testni mail poslan preko Mailpit SMTP servera."
                )
        ).subscribe().with(
                ignored -> log.info("Mail sent to {}", to),
                err -> log.error("Mail failed", err)
        );
    }

    public void sendInviteMail(Invite invite, String rawToken) {
        String inviteUrl = buildInviteUrl(rawToken);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")
                .withZone(ZoneId.of(metadataConfig.timezone()));
        String inviterName = StringUtils.defaultIfBlank(
                invite.getCreatedByAdminUsername(),
                "Administrator"
        );
        String expiresAt = dateTimeFormatter.format(invite.getExpiresAt());

        String subject = mailConfig.invite().subject();
        InviteMailContent content = inviteMailTemplateRenderer.render(new InviteMailTemplateData(
                mailConfig.invite().title(),
                inviterName,
                invite.getEmail(),
                invite.getRole().name(),
                expiresAt,
                inviteUrl,
                resolveLogoUrl()
        ));

        mailer.send(Mail.withHtml(invite.getEmail(), subject, content.html()).setText(content.text()))
                .subscribe().with(
                        ignored -> log.info("Invite mail sent for inviteId={} to {}",
                                invite.getId(), invite.getEmail()),
                        err -> log.error("Invite mail failed for inviteId={}", invite.getId(), err)
                );
    }

    public void sendAssignmentStartReminder(List<String> recipients, Assignment assignment) {
        if (CollectionUtils.isEmpty(recipients)) {
            log.info("Skipping assignment reminder for assignmentId={}"
                            + "because there are no recipients", assignment.getId());
            return;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")
                .withZone(ZoneId.of(metadataConfig.timezone()));
        String subject = "Podsjetnik: assignment " + assignment.getName() + " uskoro pocinje";
        String body =
                """
                Pozdrav,

                assignment "%s" pocinje u %s.
                Grupa: %s
                Bodovi: %d
                Kraj assignmenta: %s
                """
                .formatted(
                        assignment.getName(),
                        dateTimeFormatter.format(assignment.getStartsAt()),
                        assignment.getGroup().getName(),
                        assignment.getPoints(),
                        dateTimeFormatter.format(assignment.getEndsAt())
                );

        List<Mail> mails = recipients.stream()
                .map(recipient -> Mail.withText(recipient, subject, body))
                .toList();

        mailer.send(mails.toArray(Mail[]::new)).subscribe().with(
                ignored -> log.info("Assignment reminder sent for assignmentId={} to {} recipients",
                        assignment.getId(), recipients.size()),
                err -> log.error("Assignment reminder failed for assignmentId={}", assignment.getId(), err)
        );
    }

    private String buildInviteUrl(String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        return mailConfig.frontendBaseUrl().replaceAll("/+$", "")
                + "/register?token="
                + encodedToken;
    }

    private String resolveLogoUrl() {
        return mailConfig.logoUrl()
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> joinUrl(mailConfig.publicBaseUrl(), mailConfig.logoPath()));
    }

    private String joinUrl(String baseUrl, String path) {
        String normalizedBaseUrl = StringUtils.defaultString(baseUrl).replaceAll("/+$", "");
        String normalizedPath = StringUtils.defaultIfBlank(path, "/").replaceAll("^/+", "");

        return normalizedBaseUrl + "/" + normalizedPath;
    }
}
