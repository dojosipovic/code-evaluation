package com.codeevaluation.core.helper;

import com.codeevaluation.core.TaskListQueryParams;
import com.codeevaluation.core.api.dto.task.TaskFilterParams;
import com.codeevaluation.core.enumeration.TaskStatus;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class PagedSearchTaskImpl extends PagedSearchHelper {

    public PagedSearchTaskImpl(PagedResponseValidator pagedResponseValidator) {
        super(pagedResponseValidator);
    }

    public TaskFilterParams.TaskFilterParamsBuilder generateFilterParams(
            TaskListQueryParams taskListQueryParams, boolean allowPrivateFilters) {
        return TaskFilterParams.builder()
                .status(
                        allowPrivateFilters ? taskListQueryParams.getStatus() : TaskStatus.PUBLISHED
                )
                .enabled(taskListQueryParams.getEnabled())
                .excludeUser(taskListQueryParams.getExcludeCurrentUser())
                .shared(allowPrivateFilters ? taskListQueryParams.getShared() : Boolean.TRUE);
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
