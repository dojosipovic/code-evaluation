package com.codeevaluation.core.api.query;

import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignmentListQueryParams extends PagedParams {

    @QueryParam("groupId")
    private Long groupId;

    @QueryParam("active")
    private Boolean active;

    @QueryParam("submitted")
    private Boolean submitted;

    @QueryParam("ungraded")
    private Boolean ungraded;
}
