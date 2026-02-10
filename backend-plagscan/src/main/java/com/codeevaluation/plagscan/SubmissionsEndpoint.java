package com.codeevaluation.plagscan;

import com.codeevaluation.plagscan.dto.ScanRequest;
import com.codeevaluation.plagscan.model.PlagResult;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/jplag/submissions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SubmissionsEndpoint {

    @Inject
    JplagService jplagService;

    @POST
    @Path("/cpp")
    @Blocking
    public PlagResult scanCpp(@Valid ScanRequest request) {
        return jplagService.analyze(request);
    }
}
