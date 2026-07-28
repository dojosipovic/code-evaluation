package com.codeevaluation.core.api.resource;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.submission.SubmissionDetailResponseDto;
import com.codeevaluation.core.api.dto.submission.SubmissionListItemDto;
import com.codeevaluation.core.api.query.SubmissionListQueryParams;
import com.codeevaluation.core.service.SubmissionService;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

@Path("/api/submissions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class SubmissionResource {

    private final SubmissionService submissionService;

    @GET
    @Authenticated
    public PagedResponse<SubmissionListItemDto> getSubmissions(
            @BeanParam SubmissionListQueryParams queryParams
    ) {
        return submissionService.getSubmissions(queryParams);
    }

    @GET
    @Path("/{submissionId}")
    @Authenticated
    public SubmissionDetailResponseDto getSubmission(@PathParam("submissionId") Long submissionId) {
        return submissionService.getSubmission(submissionId);
    }
}
