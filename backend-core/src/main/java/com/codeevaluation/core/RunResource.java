package com.codeevaluation.core;

import com.codeevaluation.core.api.dto.run.RunBatchRequestDto;
import com.codeevaluation.core.api.dto.run.RunBatchResponseDto;
import com.codeevaluation.core.api.dto.run.RunRequestDto;
import com.codeevaluation.core.service.CodeExecutionService;
import com.codeevaluation.core.service.dto.RunResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

@Path("/api/run-cpp")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class RunResource {

    private final CodeExecutionService codeExecutionService;

    @POST
    public RunResult run(RunRequestDto req) {
        return codeExecutionService.run(req);
    }

    @POST
    @Path("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RunBatchResponseDto runCppBatch(RunBatchRequestDto req) {
        return codeExecutionService.runBatch(req);
    }
}
