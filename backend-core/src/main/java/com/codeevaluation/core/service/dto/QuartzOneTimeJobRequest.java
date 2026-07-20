package com.codeevaluation.core.service.dto;

import com.codeevaluation.core.job.AssignmentPostActionJob;
import com.codeevaluation.core.job.AssignmentQuartzMetadata;
import com.codeevaluation.core.job.AssignmentStartReminderJob;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import org.quartz.Job;

@Builder
public record QuartzOneTimeJobRequest(
        Instant executeAt,
        Class<? extends Job> jobClass,
        String jobName,
        String jobGroup,
        String triggerName,
        String triggerGroup,
        Map<String, String> jobData,
        String jobDescription,
        String triggerDescription,
        String actionLabel,
        String entityLabel,
        Object entityId,
        String failureMessage
) {
    public static QuartzOneTimeJobRequest assignmentStart(Long assignmentId, Instant reminderAt) {
        return QuartzOneTimeJobRequest.builder()
                .executeAt(reminderAt)
                .jobClass(AssignmentStartReminderJob.class)
                .jobName(AssignmentQuartzMetadata.reminderJobName(assignmentId))
                .jobGroup(AssignmentQuartzMetadata.reminderJobGroup())
                .triggerName(AssignmentQuartzMetadata.reminderTriggerName(assignmentId))
                .triggerGroup(AssignmentQuartzMetadata.reminderTriggerGroup())
                .jobData(
                        Map.of(AssignmentQuartzMetadata.ASSIGNMENT_ID_KEY, assignmentId.toString()))
                .jobDescription(AssignmentQuartzMetadata.REMINDER_JOB_DESCRIPTION)
                .triggerDescription(
                        AssignmentQuartzMetadata.REMINDER_TRIGGER_DESCRIPTION)
                .actionLabel(AssignmentQuartzMetadata.REMINDER_ACTION_LABEL)
                .entityLabel(AssignmentQuartzMetadata.ENTITY_LABEL)
                .entityId(assignmentId)
                .failureMessage(AssignmentQuartzMetadata.REMINDER_FAILURE_MESSAGE)
                .build();
    }

    public static QuartzOneTimeJobRequest assignmentEnd(Long assignmentId, Instant executeAt) {
        return QuartzOneTimeJobRequest.builder()
                .executeAt(executeAt)
                .jobClass(AssignmentPostActionJob.class)
                .jobName(AssignmentQuartzMetadata.postActionJobName(assignmentId))
                .jobGroup(AssignmentQuartzMetadata.postActionJobGroup())
                .triggerName(AssignmentQuartzMetadata.postActionTriggerName(assignmentId))
                .triggerGroup(AssignmentQuartzMetadata.postActionTriggerGroup())
                .jobData(
                        Map.of(AssignmentQuartzMetadata.ASSIGNMENT_ID_KEY, assignmentId.toString()))
                .jobDescription(AssignmentQuartzMetadata.POST_ACTION_JOB_DESCRIPTION)
                .triggerDescription(
                        AssignmentQuartzMetadata.POST_ACTION_TRIGGER_DESCRIPTION)
                .actionLabel(AssignmentQuartzMetadata.POST_ACTION_ACTION_LABEL)
                .entityLabel(AssignmentQuartzMetadata.ENTITY_LABEL)
                .entityId(assignmentId)
                .failureMessage(AssignmentQuartzMetadata.POST_ACTION_FAILURE_MESSAGE)
                .build();
    }
}
