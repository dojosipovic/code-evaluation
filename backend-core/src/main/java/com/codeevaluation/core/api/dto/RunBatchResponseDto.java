package com.codeevaluation.core.api.dto;

import com.codeevaluation.core.service.dto.RunResult;
import java.util.List;

public class RunBatchResponseDto {
    public String phase;           // "batch"
    public RunResult compile;      // reuse your existing RunResult DTO
    public List<TestRunResult> results;
}
