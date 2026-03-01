package com.codeevaluation.core.api.dto;


public class TestRunResult {
    public int index;
    public int exitCode;
    public long durationMs;
    public String stdout;
    public String stderr;
    public boolean timedOut;
    public String timeout; // keep same as RunResult if you want
}
