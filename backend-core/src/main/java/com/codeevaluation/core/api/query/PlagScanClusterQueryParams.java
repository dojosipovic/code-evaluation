package com.codeevaluation.core.api.query;

import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlagScanClusterQueryParams extends PagedParams {

    @QueryParam("assignmentId")
    private Long assignmentId;
}
