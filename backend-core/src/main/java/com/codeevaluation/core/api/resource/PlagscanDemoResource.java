package com.codeevaluation.core.api.resource;

import com.codeevaluation.core.client.plagscan.dto.PlagscanResult;
import com.codeevaluation.core.client.plagscan.dto.PlagscanScanRequest;
import com.codeevaluation.core.service.PlagscanService;
import io.smallrye.common.annotation.Blocking;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/plagscan/demo")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlagscanDemoResource {

    private final PlagscanService plagscanService;

    public PlagscanDemoResource(PlagscanService plagscanService) {
        this.plagscanService = plagscanService;
    }

    @POST
    @Path("/cpp")
    @Blocking
    public PlagscanResult scanCppDemo(@Valid PlagscanScanRequest request) {
        return plagscanService.scanCpp(request);
    }
}
