package com.codeevaluation.core.service;

import com.codeevaluation.core.config.AssignmentConfig;
import com.codeevaluation.core.job.AssignmentStartReminderJob;
import com.codeevaluation.core.job.AssignmentReminderQuartzMetadata;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.GroupMember;
import com.codeevaluation.core.repository.AssignmentRepository;
import com.codeevaluation.core.util.DurationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class AssignmentReminderService {

    private final AssignmentConfig assignmentConfig;
    private final AssignmentRepository assignmentRepository;
    private final MailService mailService;
    private final Instance<AssignmentReminderService> self;
    private final Scheduler scheduler;

    public void scheduleOrSendImmediately(Long assignmentId, Instant startsAt) {
        Instant now = Instant.now();
        Instant reminderAt = startsAt.minus(reminderLeadTime());

        if (!reminderAt.isAfter(now)) {
            log.info("Assignment reminder will be sent immediately for assignmentId={}",
                    assignmentId);
            self.get().sendReminder(assignmentId);
            return;
        }

        scheduleReminder(assignmentId, reminderAt);
    }

    @Transactional
    public void sendReminder(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithReminderRelations(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found for reminder: " + assignmentId));

        List<String> recipients = assignment.getGroup().getMembers().stream()
                .map(GroupMember::getUser)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .map(user -> user.getEmail())
                .distinct()
                .toList();

        mailService.sendAssignmentStartReminder(recipients, assignment);
    }

    private void scheduleReminder(Long assignmentId, Instant reminderAt) {
        JobKey jobKey = AssignmentReminderQuartzMetadata.jobKey(assignmentId);
        TriggerKey triggerKey = AssignmentReminderQuartzMetadata.triggerKey(assignmentId);

        try {
            if (scheduler.checkExists(triggerKey) || scheduler.checkExists(jobKey)) {
                log.info("Assignment reminder already scheduled for assignmentId={}", assignmentId);
                return;
            }

            JobDetail jobDetail = JobBuilder.newJob(AssignmentStartReminderJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(
                            AssignmentReminderQuartzMetadata.ASSIGNMENT_ID_KEY,
                            assignmentId.toString()
                    )
                    .withDescription("Send assignment reminder e-mails")
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobDetail)
                    .startAt(Date.from(reminderAt))
                    .withDescription("Assignment start reminder trigger")
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Assignment reminder scheduled for assignmentId={} at {}",
                    assignmentId, reminderAt);
        } catch (SchedulerException e) {
            throw new IllegalStateException(
                    "Failed to schedule assignment reminder for assignmentId=" + assignmentId, e);
        }
    }

    private Duration reminderLeadTime() {
        return DurationUtil.parseFlexibleDuration(
                assignmentConfig.reminder().leadTime()
        );
    }
}
