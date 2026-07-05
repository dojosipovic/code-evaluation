package com.codeevaluation.core.service;

import com.codeevaluation.core.config.MetadataConfig;
import com.codeevaluation.core.model.Assignment;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final MetadataConfig metadataConfig;
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

    public void sendAssignmentStartReminder(List<String> recipients, Assignment assignment) {
        if (recipients == null || recipients.isEmpty()) {
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
}
