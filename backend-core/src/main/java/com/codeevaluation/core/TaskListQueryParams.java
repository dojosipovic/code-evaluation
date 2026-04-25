package com.codeevaluation.core;

import com.codeevaluation.core.enumeration.TaskStatus;
import com.codeevaluation.core.helper.PagedParams;
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

}
