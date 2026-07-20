package com.codeevaluation.core.util;

import com.codeevaluation.core.config.LimiterConfig;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.Semaphore;

@ApplicationScoped
public class SandboxLimiter {

    private final Semaphore permits;

    public SandboxLimiter(LimiterConfig limiterConfig) {
        this.permits = new Semaphore(limiterConfig.sandbox().maxParallelRuns());
    }

    public boolean tryAcquire() {
        return  permits.tryAcquire();
    }

    public void release() {
        permits.release();
    }
}
