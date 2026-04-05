package com.codeevaluation.core.api.dto.run;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestRunResult {
    private int index;
    private int exitCode;
    private long durationMs;
    private String stdout;
    private String stderr;
    private boolean timedOut;
    private String timeout; // keep same as RunResult if you want
}
