package com.codeevaluation.core.job;

import org.quartz.JobKey;
import org.quartz.TriggerKey;

public final class AssignmentReminderQuartzMetadata {

    public static final String JOB_GROUP = "assignment-reminders";
    public static final String TRIGGER_GROUP = "assignment-reminder-triggers";
    public static final String ASSIGNMENT_ID_KEY = "assignmentId";

    private AssignmentReminderQuartzMetadata() {}

    public static JobKey jobKey(Long assignmentId) {
        return new JobKey(jobName(assignmentId), JOB_GROUP);
    }

    public static TriggerKey triggerKey(Long assignmentId) {
        return new TriggerKey(triggerName(assignmentId), TRIGGER_GROUP);
    }

    public static String jobName(Long assignmentId) {
        return "assignment-reminder-job-" + assignmentId;
    }

    public static String triggerName(Long assignmentId) {
        return "assignment-reminder-trigger-" + assignmentId;
    }
}
