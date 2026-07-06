package com.codeevaluation.core.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "assignment")
public interface AssignmentConfig {

    Reminder reminder();

    PostAction postAction();

    interface Reminder {
        String leadTime();
    }

    interface PostAction {
        String delay();
    }
}
