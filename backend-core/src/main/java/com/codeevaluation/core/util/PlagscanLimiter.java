package com.codeevaluation.core.util;

import com.codeevaluation.core.config.LimiterConfig;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.Semaphore;

@ApplicationScoped
public class PlagscanLimiter {

    private final Semaphore permits;

    public PlagscanLimiter(LimiterConfig limiterConfig) {
        this.permits = new Semaphore(limiterConfig.plagScan().maxParallelRuns());
    }

    public boolean tryAcquire() {
        return permits.tryAcquire();
    }

    public void release() {
        permits.release();
    }
}
