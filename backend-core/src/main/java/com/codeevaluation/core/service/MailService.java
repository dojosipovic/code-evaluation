package com.codeevaluation.core.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class MailService {

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
}
