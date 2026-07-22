package com.codeevaluation.core.api.resource;

import com.codeevaluation.core.api.dto.dashboard.DashboardDto;
import com.codeevaluation.core.service.DashboardService;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

@Path("/api/dashboard")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class DashboardResource {

    private final DashboardService dashboardService;

    @GET
    @Authenticated
    public DashboardDto getDashboard() {
        return dashboardService.getDashboard();
    }
}
