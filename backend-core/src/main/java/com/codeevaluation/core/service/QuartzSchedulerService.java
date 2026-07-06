package com.codeevaluation.core.service;

import com.codeevaluation.core.service.dto.QuartzOneTimeJobRequest;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
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
public class QuartzSchedulerService {

    private final Scheduler scheduler;

    public void scheduleOneTimeJob(QuartzOneTimeJobRequest request) {
        JobKey jobKey = new JobKey(request.jobName(), request.jobGroup());
        TriggerKey triggerKey = new TriggerKey(request.triggerName(), request.triggerGroup());

        try {
            if (scheduler.checkExists(triggerKey) || scheduler.checkExists(jobKey)) {
                log.info("{} already scheduled for {}Id={}", request.actionLabel(),
                        request.entityLabel(), request.entityId());
                return;
            }

            JobDetail jobDetail = JobBuilder.newJob(request.jobClass())
                    .withIdentity(jobKey)
                    .usingJobData(new JobDataMap(request.jobData()))
                    .withDescription(request.jobDescription())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobDetail)
                    .startAt(Date.from(request.executeAt()))
                    .withDescription(request.triggerDescription())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("{} scheduled for {}Id={} at {}", request.actionLabel(),
                    request.entityLabel(), request.entityId(), request.executeAt());
        } catch (SchedulerException e) {
            throw new IllegalStateException(
                    request.failureMessage() + " for " + request.entityLabel() + "Id="
                            + request.entityId(),
                    e
            );
        }
    }
}
