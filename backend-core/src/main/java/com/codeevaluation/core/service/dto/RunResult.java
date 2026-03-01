package com.codeevaluation.core.service.dto;

import java.time.Duration;

public class RunResult {
    public int exitCode;
    public long durationMs;
    public String stdout;
    public String stderr;
    public boolean timedOut;
    public String timeout;
    public String phase;

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
        r.stderr = "";
        r.timedOut = true;
        r.timeout = d.toString();
        return r;
    }
}
