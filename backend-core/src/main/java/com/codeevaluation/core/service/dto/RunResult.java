package com.codeevaluation.core.service.dto;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RunResult {
    private int exitCode;
    private long durationMs;
    private String stdout;
    private String stderr;
    private boolean timedOut;
    private String timeout;
    private String phase;

    public RunResult() {}

    public RunResult(int exitCode, long durationMs, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.durationMs = durationMs;
        this.stdout = stdout;
        this.stderr = stderr;
        this.timedOut = false;
    }

    public static RunResult timeout(Duration d) {
        RunResult r = new RunResult();
        r.exitCode = -1;
        r.durationMs = d.toMillis();
        r.stdout = "";
        r.stderr = "Time limit exceeded";
        r.timedOut = true;
        r.timeout = formatTimeout(d);
        return r;
    }

    private static String formatTimeout(Duration timeout) {
        long millis = timeout.toMillis();
        if (millis % 1000 == 0) {
            return (millis / 1000) + "s";
        }
        return millis + "ms";
    }
}
