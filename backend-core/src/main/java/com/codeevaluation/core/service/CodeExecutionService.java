package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.run.RunBatchResponseDto;
import com.codeevaluation.core.api.dto.run.RunRequestDto;
import com.codeevaluation.core.api.dto.run.TestCase;
import com.codeevaluation.core.code.CodeRunner;
import com.codeevaluation.core.code.CodeRunnerFactory;
import com.codeevaluation.core.enumeration.ProgrammingLanguage;
import com.codeevaluation.core.service.dto.RunResult;
import com.codeevaluation.core.util.SandboxLimiter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@RequiredArgsConstructor
public class CodeExecutionService {

    private final CodeRunnerFactory codeRunnerFactory;
    private final SandboxLimiter sandboxLimiter;

    private static final int DEFAULT_TIMEOUT_SEC = 5;
    private static final int MAX_PARALLEL_EXECUTIONS = 3;

    public RunBatchResponseDto runBatch(String code, List<TestCase> tests) {
        if (!sandboxLimiter.tryAcquire()) {
            throw new WebApplicationException(
                    "Too many concurrent executions",
                    Response.Status.TOO_MANY_REQUESTS
            );
        }

        try {
            List<String> inputs = tests.stream()
                    .map(t -> StringUtils.defaultIfEmpty(t.input(), ""))
                    .toList();
            CodeRunner codeRunner = codeRunnerFactory.getRunner(ProgrammingLanguage.CPP);

            return codeRunner.runBatch(
                    code, inputs, DEFAULT_TIMEOUT_SEC, MAX_PARALLEL_EXECUTIONS
            );
        } finally {
            sandboxLimiter.release();
        }
    }

    public RunResult run(RunRequestDto req) {
        if (!sandboxLimiter.tryAcquire()) {
            throw new WebApplicationException(
                    "Too many concurrent executions",
                    Response.Status.TOO_MANY_REQUESTS
            );
        }

        try {
            if (req == null || req.code() == null || req.code().isBlank()) {
                throw new BadRequestException("Missing code");
            }
            CodeRunner codeRunner = codeRunnerFactory.getRunner(ProgrammingLanguage.CPP);

            return codeRunner.run(req.code(), req.input(), DEFAULT_TIMEOUT_SEC);
        } finally {
            sandboxLimiter.release();
        }
    }
}
