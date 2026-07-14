package com.codeevaluation.core.helper;

import com.codeevaluation.core.api.dto.plagscan.PlagScanClusterFilterParams;
import com.codeevaluation.core.api.query.PlagScanClusterQueryParams;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class PagedSearchPlagScanClusterImpl extends PagedSearchHelper {

    public PagedSearchPlagScanClusterImpl(PagedResponseValidator pagedResponseValidator) {
        super(pagedResponseValidator);
    }

    public PlagScanClusterFilterParams.PlagScanClusterFilterParamsBuilder generateFilterParams(
            PlagScanClusterQueryParams queryParams
    ) {
        return PlagScanClusterFilterParams.builder()
                .assignmentId(queryParams.getAssignmentId());
    }

    @Override
    public Map<String, String> sortFieldMappings() {
        return Map.of(
                "id", "c.id",
                "assignmentId", "a.id",
                "similarity", "c.similarity",
                "createdAt", "c.createdAt"
        );
    }
}
