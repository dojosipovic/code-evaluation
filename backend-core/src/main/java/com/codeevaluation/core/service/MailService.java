package com.codeevaluation.core.service;

import com.codeevaluation.core.config.MailConfig;
import com.codeevaluation.core.config.MetadataConfig;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Invite;
import com.codeevaluation.core.model.Submission;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.service.mail.AssignmentPostActionMailTemplateData;
import com.codeevaluation.core.service.mail.AssignmentPostActionMailTemplateRenderer;
import com.codeevaluation.core.service.mail.AssignmentStartReminderMailTemplateData;
import com.codeevaluation.core.service.mail.AssignmentStartReminderMailTemplateRenderer;
import com.codeevaluation.core.service.mail.InviteMailTemplateData;
import com.codeevaluation.core.service.mail.InviteMailTemplateRenderer;
import com.codeevaluation.core.service.mail.MailContent;
import com.codeevaluation.core.service.mail.SubmissionEvaluatedMailTemplateData;
import com.codeevaluation.core.service.mail.SubmissionEvaluatedMailTemplateRenderer;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    private final AssignmentStartReminderMailTemplateRenderer
            assignmentStartReminderMailTemplateRenderer;
    private final AssignmentPostActionMailTemplateRenderer assignmentPostActionMailTemplateRenderer;
    private final SubmissionEvaluatedMailTemplateRenderer submissionEvaluatedMailTemplateRenderer;
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
        MailContent content = inviteMailTemplateRenderer.render(new InviteMailTemplateData(
                mailConfig.invite().title(),
                inviterName,
                invite.getEmail(),
                invite.getRole().name(),
                expiresAt,
                inviteUrl,
                resolveLogoUrl()
        ));

        mailer.send(Mail.withHtml(
                    invite.getEmail(), subject, content.html()).setText(content.text())
                )
                .subscribe().with(
                        ignored -> log.info("Invite mail sent for inviteId={} to {}",
                                invite.getId(), invite.getEmail()),
                        err -> log.error("Invite mail failed for inviteId={}", invite.getId(), err)
                );
    }

    public void sendSubmissionEvaluatedNotifications(List<Submission> submissions) {
        if (CollectionUtils.isEmpty(submissions)) {
            log.info(
                    "Skipping submission evaluated notifications because there are no submissions"
            );
            return;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")
                .withZone(ZoneId.of(metadataConfig.timezone()));
        List<Mail> mails = submissions.stream()
                .filter(submission -> submission.getFinalScore() != null)
                .filter(submission -> submission.getUser() != null)
                .filter(submission -> isEnabledRecipient(submission.getUser()))
                .filter(submission -> StringUtils.isNotBlank(submission.getUser().getEmail()))
                .map(submission -> buildSubmissionEvaluatedMail(submission, dateTimeFormatter))
                .toList();

        if (mails.isEmpty()) {
            log.info("Skipping submission evaluated notifications because no submission has "
                    + "a valid recipient");
            return;
        }

        mailer.send(mails.toArray(Mail[]::new)).subscribe().with(
                ignored -> log.info("Submission evaluated notifications sent to {} recipients",
                        mails.size()),
                err -> log.error("Submission evaluated notifications failed", err)
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
        String assignmentUrl = buildAssignmentUrl(assignment.getId());
        String assignmentName = assignment.getName();
        String subject = renderAssignmentText(
                mailConfig.assignmentStartReminder().subject(), assignmentName);
        MailContent content = assignmentStartReminderMailTemplateRenderer.render(
                new AssignmentStartReminderMailTemplateData(
                        renderAssignmentText(
                                mailConfig.assignmentStartReminder().title(), assignmentName),
                        assignmentName,
                        assignment.getTask().getTitle(),
                        assignment.getGroup().getName(),
                        dateTimeFormatter.format(assignment.getStartsAt()),
                        dateTimeFormatter.format(assignment.getEndsAt()),
                        assignment.getPoints(),
                        assignmentUrl,
                        resolveLogoUrl()
                ));

        List<Mail> mails = recipients.stream()
                .map(recipient -> Mail.withHtml(recipient, subject, content.html())
                        .setText(content.text()))
                .toList();

        mailer.send(mails.toArray(Mail[]::new)).subscribe().with(
                ignored -> log.info("Assignment reminder sent for assignmentId={} to {} recipients",
                        assignment.getId(), recipients.size()),
                err -> log.error(
                        "Assignment reminder failed for assignmentId={}", assignment.getId(), err)
        );
    }

    public void sendAssignmentPostActionNotification(
            String recipient,
            boolean recipientEnabled,
            String professorName,
            Long assignmentId,
            String assignmentName,
            String taskTitle,
            String groupName,
            Instant startsAt,
            Instant endsAt,
            Integer points,
            int submissionCount
    ) {
        if (!recipientEnabled) {
            log.info("Skipping assignment post-action notification for assignmentId={} "
                    + "because professor is disabled", assignmentId);
            return;
        }

        if (StringUtils.isBlank(recipient)) {
            log.info("Skipping assignment post-action notification for assignmentId={} "
                            + "because professor email is blank", assignmentId);
            return;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm")
                .withZone(ZoneId.of(metadataConfig.timezone()));
        String subject = renderAssignmentText(
                mailConfig.assignmentPostAction().subject(), assignmentName);
        MailContent content = assignmentPostActionMailTemplateRenderer.render(
                new AssignmentPostActionMailTemplateData(
                        renderAssignmentText(
                                mailConfig.assignmentPostAction().title(), assignmentName),
                        StringUtils.defaultIfBlank(professorName, "profesore"),
                        assignmentName,
                        taskTitle,
                        groupName,
                        dateTimeFormatter.format(startsAt),
                        dateTimeFormatter.format(endsAt),
                        points,
                        submissionCount,
                        buildAssignmentEvaluationUrl(assignmentId),
                        resolveLogoUrl()
                ));

        mailer.send(Mail.withHtml(recipient, subject, content.html()).setText(content.text()))
                .subscribe().with(
                        ignored -> log.info("Assignment post-action notification sent for "
                                + "assignmentId={} to {}", assignmentId, recipient),
                        err -> log.error("Assignment post-action notification failed for "
                                + "assignmentId={}", assignmentId, err)
                );
    }

    private String buildInviteUrl(String rawToken) {
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        return mailConfig.frontendBaseUrl().replaceAll("/+$", "")
                + "/register?token="
                + encodedToken;
    }

    private String buildAssignmentUrl(Long assignmentId) {
        return mailConfig.frontendBaseUrl().replaceAll("/+$", "")
                + "/assignment/"
                + assignmentId
                + "/solve";
    }

    private String buildAssignmentEvaluationUrl(Long assignmentId) {
        return mailConfig.frontendBaseUrl().replaceAll("/+$", "")
                + "/assignments/"
                + assignmentId
                + "/evaluate";
    }

    private Mail buildSubmissionEvaluatedMail(
            Submission submission,
            DateTimeFormatter dateTimeFormatter
    ) {
        String assignmentName = submission.getAssignment().getName();
        String subject = renderAssignmentText(
                mailConfig.submissionEvaluated().subject(), assignmentName);
        MailContent content = submissionEvaluatedMailTemplateRenderer.render(
                new SubmissionEvaluatedMailTemplateData(
                        renderAssignmentText(
                                mailConfig.submissionEvaluated().title(), assignmentName),
                        displayName(submission.getUser(), "studentu"),
                        assignmentName,
                        submission.getTask().getTitle(),
                        formatScore(submission.getFinalScore()),
                        submission.getAssignment().getPoints(),
                        dateTimeFormatter.format(submission.getSubmittedAt()),
                        buildSubmissionUrl(submission.getId()),
                        resolveLogoUrl()
                ));

        return Mail.withHtml(submission.getUser().getEmail(), subject, content.html())
                .setText(content.text());
    }

    private String buildSubmissionUrl(Long submissionId) {
        return mailConfig.frontendBaseUrl().replaceAll("/+$", "")
                + "/submissions/"
                + submissionId;
    }

    private String renderAssignmentText(String value, String assignmentName) {
        String normalizedAssignmentName = StringUtils.defaultString(assignmentName);

        return StringUtils.defaultString(value)
                .replace("%assignmentName%", normalizedAssignmentName)
                .replace("{assignmentName}", normalizedAssignmentName);
    }

    private String displayName(User user, String fallback) {
        String fullName = "%s %s".formatted(
                StringUtils.defaultString(user.getFirstname()).trim(),
                StringUtils.defaultString(user.getLastname()).trim()
        ).trim();

        return StringUtils.firstNonBlank(fullName, user.getUsername(), fallback);
    }

    private boolean isEnabledRecipient(User user) {
        return Boolean.TRUE.equals(user.getEnabled());
    }

    private String formatScore(BigDecimal score) {
        return score.stripTrailingZeros().toPlainString();
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
