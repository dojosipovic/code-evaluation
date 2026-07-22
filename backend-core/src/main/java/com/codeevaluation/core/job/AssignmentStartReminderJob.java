package com.codeevaluation.core.job;

import com.codeevaluation.core.service.AssignmentTimedActionService;
import jakarta.enterprise.context.Dependent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

@Slf4j
@Dependent
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class AssignmentStartReminderJob implements Job {

    private final AssignmentTimedActionService assignmentTimedActionService;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        long assignmentId = Long.parseLong(
                jobDataMap.getString(AssignmentQuartzMetadata.ASSIGNMENT_ID_KEY)
        );

        log.info("Executing assignment reminder job for assignmentId={}", assignmentId);
        assignmentTimedActionService.sendStartReminder(assignmentId);
    }
}
