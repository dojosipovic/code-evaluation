package com.codeevaluation.core.job;

import com.codeevaluation.core.service.InviteService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public class InviteStatusJob {

    private final InviteService inviteService;

    @Scheduled(cron = "{invite.status-expired.cron.expression}")
    void inviteStatusExpiredJob() {
        log.info("inviteStatusExpiredJob start");
        long rowsAffected = inviteService.expirePendingInvites();
        log.info("Set {} invites to expired", rowsAffected);
        log.info("inviteStatusExpiredJob finish");
    }
}
