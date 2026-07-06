package com.codeevaluation.core.service;

import com.codeevaluation.core.config.AssignmentConfig;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.GroupMember;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.repository.AssignmentRepository;
import com.codeevaluation.core.service.dto.QuartzOneTimeJobRequest;
import com.codeevaluation.core.util.DurationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class AssignmentTimedActionService {

    private final AssignmentConfig assignmentConfig;
    private final AssignmentRepository assignmentRepository;
    private final MailService mailService;
    private final Instance<AssignmentTimedActionService> self;
    private final QuartzSchedulerService quartzSchedulerService;

    public void scheduleOnCreated(Long assignmentId, Instant startsAt, Instant endsAt) {
        scheduleStartReminderOrSendImmediately(assignmentId, startsAt);
        schedulePostAction(assignmentId, endsAt);
    }

    @Transactional
    public void sendStartReminder(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithReminderRelations(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found for reminder: " + assignmentId));

        List<String> recipients = assignment.getGroup().getMembers().stream()
                .map(GroupMember::getUser)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .map(User::getEmail)
                .distinct()
                .toList();

        mailService.sendAssignmentStartReminder(recipients, assignment);
    }

    public void executePostAction(Long assignmentId) {
        log.info("Assignment post-action hook executed for assignmentId={}", assignmentId);
    }

    private void scheduleStartReminderOrSendImmediately(Long assignmentId, Instant startsAt) {
        Instant reminderAt = startsAt.minus(reminderLeadTime());
        if (!reminderAt.isAfter(Instant.now())) {
            log.info("Assignment reminder will be sent immediately for assignmentId={}",
                    assignmentId);
            self.get().sendStartReminder(assignmentId);
            return;
        }

        quartzSchedulerService.scheduleOneTimeJob(
                QuartzOneTimeJobRequest.assignmentStart(assignmentId, reminderAt)
        );
    }

    private void schedulePostAction(Long assignmentId, Instant endsAt) {
        Instant executeAt = endsAt.plus(postActionDelay());
        quartzSchedulerService.scheduleOneTimeJob(
                QuartzOneTimeJobRequest.assignmentEnd(assignmentId, executeAt)
        );
    }

    private Duration reminderLeadTime() {
        return DurationUtil.parseFlexibleDuration(assignmentConfig.reminder().leadTime());
    }

    private Duration postActionDelay() {
        return DurationUtil.parseFlexibleDuration(assignmentConfig.postAction().delay());
    }
}
