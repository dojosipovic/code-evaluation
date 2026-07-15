package com.codeevaluation.core.api.resource;

import com.codeevaluation.core.api.dto.assignment.AssignmentEvaluateRequestDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentResponseDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentRunRequestDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentRunResponseDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentSubmitRequestDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentSubmitResponseDto;
import com.codeevaluation.core.api.dto.submission.SubmissionResponseDto;
import com.codeevaluation.core.service.AssignmentService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Path("/api/assignments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class AssignmentResource {

    private final AssignmentService assignmentService;

    @GET
    @Path("/{assignmentId}")
    @Authenticated
    public AssignmentResponseDto getAssignment(@PathParam("assignmentId") Long assignmentId) {
        return assignmentService.get(assignmentId);
    }

    @POST
    @Path("/{assignmentId}/run")
    @Authenticated
    public AssignmentRunResponseDto runCode(
            @PathParam("assignmentId") Long assignmentId,
            AssignmentRunRequestDto req) {
        return assignmentService.runAssignment(assignmentId, req);
    }

    @POST
    @Path("/{assignmentId}/submit")
    @Authenticated
    public AssignmentSubmitResponseDto submitAssignment(
            @PathParam("assignmentId") Long assignmentId,
            AssignmentSubmitRequestDto req) {
        return assignmentService.submitAssignment(assignmentId, req);
    }

    @POST
    @Path("/{assignmentId}/evaluate")
    @RolesAllowed({"ADMIN", "PROF"})
    public List<SubmissionResponseDto> evaluateAssignment(
            @PathParam("assignmentId") Long assignmentId,
            AssignmentEvaluateRequestDto req) {
        return assignmentService.evaluateAssignment(assignmentId, req);
    }
}
