package com.codeevaluation.core.helper;

import com.codeevaluation.core.TaskListQueryParams;
import com.codeevaluation.core.api.dto.task.TaskFilterParams;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class PagedSearchTaskImpl extends PagedSearchHelper {

    public PagedSearchTaskImpl(PagedResponseValidator pagedResponseValidator) {
        super(pagedResponseValidator);
    }

    public TaskFilterParams.TaskFilterParamsBuilder generateFilterParams(
            TaskListQueryParams taskListQueryParams) {
        return TaskFilterParams.builder()
                .status(taskListQueryParams.getStatus())
                .enabled(taskListQueryParams.getEnabled())
                .excludeUser(taskListQueryParams.getExcludeCurrentUser())
                .shared(taskListQueryParams.getShared());
    }

    @Override
    public Map<String, String> sortFieldMappings() {
        return Map.of(
                "id", "t.id",
                "title", "t.title",
                "status", "t.status",
                "enabled", "t.enabled"
        );
    }
}
