package com.codeevaluation.core.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "assignment")
public interface AssignmentConfig {

    Reminder reminder();

    interface Reminder {
        String leadTime();
    }
}
