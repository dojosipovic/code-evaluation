package com.codeevaluation.core.helper;

import com.codeevaluation.core.api.dto.assignment.AssignmentFilterParams;
import com.codeevaluation.core.api.query.AssignmentListQueryParams;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class PagedSearchAssignmentImpl extends PagedSearchHelper {

    public PagedSearchAssignmentImpl(PagedResponseValidator pagedResponseValidator) {
        super(pagedResponseValidator);
    }

    public AssignmentFilterParams.AssignmentFilterParamsBuilder generateFilterParams(
            AssignmentListQueryParams assignmentListQueryParams) {
        return AssignmentFilterParams.builder()
                .groupId(assignmentListQueryParams.getGroupId())
                .active(assignmentListQueryParams.getActive())
                .submitted(assignmentListQueryParams.getSubmitted())
                .ungraded(assignmentListQueryParams.getUngraded());
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
