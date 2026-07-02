package com.codeevaluation.core.helper;

import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class PagedSearchAssignmentImpl extends PagedSearchHelper {

    public PagedSearchAssignmentImpl(PagedResponseValidator pagedResponseValidator) {
        super(pagedResponseValidator);
    }

    @Override
    public Map<String, String> sortFieldMappings() {
        return Map.of(
                "id", "a.id",
                "name", "a.name",
                "startsAt", "a.startsAt",
                "endsAt", "a.endsAt",
                "points", "a.points",
                "taskTitle", "task.title",
                "createdBy", "createdBy.lastname"
        );
    }
}
