package com.codeevaluation.core.api.resource;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.plagscan.PlagScanClusterDto;
import com.codeevaluation.core.api.query.PlagScanClusterQueryParams;
import com.codeevaluation.core.service.PlagScanClusterService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

@Path("/api/plagscan")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class PlagScanResource {

    private final PlagScanClusterService plagScanClusterService;

    @GET
    @Path("/clusters")
    @RolesAllowed({"ADMIN", "PROF"})
    public PagedResponse<PlagScanClusterDto> getClusters(
            @BeanParam PlagScanClusterQueryParams queryParams
    ) {
        return plagScanClusterService.getClusters(queryParams);
    }
}
