package com.codeevaluation.core.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "assignment")
public interface AssignmentConfig {

    Reminder reminder();

    interface Reminder {
        String leadTime();
    }
}
