package com.codeevaluation.core.code;

import com.codeevaluation.core.api.dto.run.RunBatchResponseDto;
import com.codeevaluation.core.code.sandbox.CppDockerSandbox;
import com.codeevaluation.core.enumeration.ProgrammingLanguage;
import com.codeevaluation.core.service.dto.RunResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class CppCodeRunner implements CodeRunner {

    private final CppDockerSandbox sandbox;

    @Override
    public ProgrammingLanguage language() {
        return ProgrammingLanguage.CPP;
    }

    @Override
    public RunBatchResponseDto runBatch(String code, List<String> inputs, int timeoutSecPerTest,
                                        int maxParallel) {
        return sandbox.compileAndRunBatchParallel(code, inputs, timeoutSecPerTest, maxParallel);
    }

    @Override
    public RunResult run(String code, String input, int timeoutSec) {
        return sandbox.compileAndRun(code, input, timeoutSec);
    }
}
