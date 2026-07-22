package com.codeevaluation.core.job;

public final class AssignmentQuartzMetadata {

    public static final String ASSIGNMENT_ID_KEY = "assignmentId";
    public static final String ENTITY_LABEL = "assignment";

    public static final String REMINDER_JOB_DESCRIPTION = "Send assignment reminder e-mails";
    public static final String REMINDER_TRIGGER_DESCRIPTION =
            "Assignment start reminder trigger";
    public static final String REMINDER_ACTION_LABEL = "Assignment reminder";
    public static final String REMINDER_FAILURE_MESSAGE =
            "Failed to schedule assignment reminder";

    public static final String POST_ACTION_JOB_DESCRIPTION = "Execute assignment post-action";
    public static final String POST_ACTION_TRIGGER_DESCRIPTION =
            "Assignment post-action trigger";
    public static final String POST_ACTION_ACTION_LABEL = "Assignment post-action";
    public static final String POST_ACTION_FAILURE_MESSAGE =
            "Failed to schedule assignment post-action";

    private static final String REMINDER_JOB_GROUP = "assignment-reminders";
    private static final String REMINDER_JOB_NAME_PREFIX = "assignment-reminder-job-";
    private static final String REMINDER_TRIGGER_GROUP = "assignment-reminder-triggers";
    private static final String REMINDER_TRIGGER_NAME_PREFIX = "assignment-reminder-trigger-";
    private static final String POST_ACTION_JOB_GROUP = "assignment-post-actions";
    private static final String POST_ACTION_JOB_NAME_PREFIX = "assignment-post-action-job-";
    private static final String POST_ACTION_TRIGGER_GROUP = "assignment-post-action-triggers";
    private static final String POST_ACTION_TRIGGER_NAME_PREFIX = "assignment-post-action-trigger-";

    private AssignmentQuartzMetadata() {}

    public static String reminderJobName(Long assignmentId) {
        return REMINDER_JOB_NAME_PREFIX + assignmentId;
    }

    public static String reminderJobGroup() {
        return REMINDER_JOB_GROUP;
    }

    public static String reminderTriggerName(Long assignmentId) {
        return REMINDER_TRIGGER_NAME_PREFIX + assignmentId;
    }

    public static String reminderTriggerGroup() {
        return REMINDER_TRIGGER_GROUP;
    }

    public static String postActionJobName(Long assignmentId) {
        return POST_ACTION_JOB_NAME_PREFIX + assignmentId;
    }

    public static String postActionJobGroup() {
        return POST_ACTION_JOB_GROUP;
    }

    public static String postActionTriggerName(Long assignmentId) {
        return POST_ACTION_TRIGGER_NAME_PREFIX + assignmentId;
    }

    public static String postActionTriggerGroup() {
        return POST_ACTION_TRIGGER_GROUP;
    }
}
