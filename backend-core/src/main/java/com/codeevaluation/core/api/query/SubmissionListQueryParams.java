package com.codeevaluation.core.api.query;

import com.codeevaluation.core.enumeration.SubmissionStatus;
import jakarta.ws.rs.QueryParam;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionListQueryParams extends PagedParams {

    @QueryParam("assignmentId")
    private Long assignmentId;

    @QueryParam("userId")
    private Long userId;

    @QueryParam("status")
    private SubmissionStatus status;

    @QueryParam("submittedAfter")
    private Instant submittedAfter;

    @QueryParam("submittedBefore")
    private Instant submittedBefore;
}
