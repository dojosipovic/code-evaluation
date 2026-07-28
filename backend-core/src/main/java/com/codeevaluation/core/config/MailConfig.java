package com.codeevaluation.core.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "mail")
public interface MailConfig {

    String frontendBaseUrl();

    String publicBaseUrl();

    String logoPath();

    Optional<String> logoUrl();

    Invite invite();

    AssignmentStartReminder assignmentStartReminder();

    AssignmentPostAction assignmentPostAction();

    SubmissionEvaluated submissionEvaluated();

    interface Invite {

        String subject();

        String title();
    }

    interface AssignmentStartReminder {

        String subject();

        String title();
    }

    interface AssignmentPostAction {

        String subject();

        String title();
    }

    interface SubmissionEvaluated {

        String subject();

        String title();
    }
}
