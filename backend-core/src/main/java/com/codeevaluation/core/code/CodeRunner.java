package com.codeevaluation.core.code;

import com.codeevaluation.core.api.dto.run.RunBatchResponseDto;
import com.codeevaluation.core.enumeration.ProgrammingLanguage;
import com.codeevaluation.core.service.dto.RunResult;
import java.util.List;

public interface CodeRunner {

    ProgrammingLanguage language();

    RunBatchResponseDto runBatch(
            String code,
            List<String> inputs,
            int timeoutSecPerTest,
            int maxParallel
    );

    RunResult run(String code, String input, int timeoutSec);
}
