package com.codeevaluation.core.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "metadata")
public interface MetadataConfig {

    String timezone();
}
