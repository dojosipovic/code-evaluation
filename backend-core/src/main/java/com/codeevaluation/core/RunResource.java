package com.codeevaluation.core;

import com.codeevaluation.core.api.dto.RunBatchRequestDto;
import com.codeevaluation.core.api.dto.RunBatchResponseDto;
import com.codeevaluation.core.api.dto.RunRequestDto;
import com.codeevaluation.core.service.CppDockerSandboxService;
import com.codeevaluation.core.service.dto.RunResult;
import com.codeevaluation.core.util.SandboxLimiter;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Path("/run-cpp")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class RunResource {

    private final CppDockerSandboxService svc;
    private final SandboxLimiter limiter;

    @POST
    public RunResult run(RunRequestDto req) {
        if (req == null || req.code() == null || req.code().isBlank()) {
            throw new BadRequestException("Missing code");
        }
        return svc.compileAndRun(req.code(), req.input(), req.timeoutSec());
    }

    @POST
    @Path("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RunBatchResponseDto runCppBatch(RunBatchRequestDto req) {

        if (!limiter.tryAcquire()) {
            throw new WebApplicationException(
                    "Too many concurrent executions",
                    Response.Status.TOO_MANY_REQUESTS
            );
        }

        try {
            int timeout = req.timeoutSec() == null ? 5 : req.timeoutSec();
            List<String> inputs = req.tests().stream()
                    .map(t -> t.input() == null ? "" : t.input())
                    .toList();

            return svc.compileAndRunBatchParallel(req.code(), inputs, timeout, 3);
        } finally {
            limiter.release();
        }

    }
}
