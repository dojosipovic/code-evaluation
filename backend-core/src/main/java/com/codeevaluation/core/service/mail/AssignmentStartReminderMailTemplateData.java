package com.codeevaluation.core.service.mail;

public record AssignmentStartReminderMailTemplateData(
        String title,
        String assignmentName,
        String taskTitle,
        String groupName,
        String startsAt,
        String endsAt,
        Integer points,
        String assignmentUrl,
        String logoUrl
) {
}
