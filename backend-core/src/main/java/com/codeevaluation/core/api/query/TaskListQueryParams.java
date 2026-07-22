package com.codeevaluation.core.api.query;

import com.codeevaluation.core.enumeration.TaskStatus;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskListQueryParams extends PagedParams {

    @QueryParam("status")
    private TaskStatus status;

    @QueryParam("enabled")
    private Boolean enabled;

    @QueryParam("shared")
    private Boolean shared;

    @QueryParam("excludeCurrentUser")
    private Boolean excludeCurrentUser;

}
