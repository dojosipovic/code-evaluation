package com.codeevaluation.core.api.dto.run;

import com.codeevaluation.core.service.dto.RunResult;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RunBatchResponseDto {
    private String phase;           // "batch"
    private RunResult compile;      // reuse your existing RunResult DTO
    private List<TestRunResult> results;
}
