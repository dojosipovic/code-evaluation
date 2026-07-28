package com.codeevaluation.core.service.mail;

public record AssignmentPostActionMailTemplateData(
        String title,
        String professorName,
        String assignmentName,
        String taskTitle,
        String groupName,
        String startsAt,
        String endsAt,
        Integer points,
        int submissionCount,
        String evaluationUrl,
        String logoUrl
) {
}
