package com.codeevaluation.core.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "limiter")
public interface LimiterConfig {

    PlagScan plagScan();

    Sandbox sandbox();

    interface PlagScan {
        int maxParallelRuns();
    }

    interface Sandbox {
        int maxParallelRuns();
    }
}
