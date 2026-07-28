package com.codeevaluation.core.service.mail;

public record SubmissionEvaluatedMailTemplateData(
        String title,
        String studentName,
        String assignmentName,
        String taskTitle,
        String finalScore,
        Integer maxPoints,
        String submittedAt,
        String submissionUrl,
        String logoUrl
) {
}
