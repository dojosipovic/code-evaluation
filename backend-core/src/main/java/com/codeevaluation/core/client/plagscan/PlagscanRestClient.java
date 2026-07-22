package com.codeevaluation.core.client.plagscan;

import com.codeevaluation.core.client.plagscan.dto.PlagscanResult;
import com.codeevaluation.core.client.plagscan.dto.PlagscanScanRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/jplag/submissions")
@RegisterRestClient(configKey = "plagscan-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface PlagscanRestClient {

    @POST
    @Path("/cpp")
    PlagscanResult scanCpp(@Valid PlagscanScanRequest request);
}
