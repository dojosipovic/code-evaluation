package com.codeevaluation.core.api.resource;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.plagscan.PlagScanClusterDto;
import com.codeevaluation.core.api.dto.plagscan.PlagScanReportFileDownload;
import com.codeevaluation.core.api.query.PlagScanClusterQueryParams;
import com.codeevaluation.core.service.PlagScanClusterService;
import com.codeevaluation.core.service.PlagScanReportFileService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Path("/api/plagscan")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class PlagScanResource {

    private final PlagScanClusterService plagScanClusterService;
    private final PlagScanReportFileService plagScanReportFileService;

    @GET
    @Path("/clusters")
    @RolesAllowed({"ADMIN", "PROF"})
    public PagedResponse<PlagScanClusterDto> getClusters(
            @BeanParam PlagScanClusterQueryParams queryParams
    ) {
        return plagScanClusterService.getClusters(queryParams);
    }

    @HEAD
    @Path("/report/exists/{assignmentId}")
    @RolesAllowed({"ADMIN", "PROF"})
    public Response reportExists(
            @PathParam("assignmentId") Long assignmentId
    ) {
        if (plagScanReportFileService.reportExists(assignmentId)) {
            return Response.ok().build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/report")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getReportFile(@QueryParam("token") String token) {
        PlagScanReportFileDownload reportFile = plagScanReportFileService.getReportFile(token);

        return Response.ok(reportFile.bytes(), MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s\"".formatted(reportFile.filename())
                )
                .header(HttpHeaders.CONTENT_LENGTH, reportFile.bytes().length)
                .build();
    }
}
