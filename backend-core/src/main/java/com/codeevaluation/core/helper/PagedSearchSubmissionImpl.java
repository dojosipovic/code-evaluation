package com.codeevaluation.core.helper;

import com.codeevaluation.core.api.dto.submission.SubmissionFilterParams;
import com.codeevaluation.core.api.query.SubmissionListQueryParams;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class PagedSearchSubmissionImpl extends PagedSearchHelper {

    public PagedSearchSubmissionImpl(PagedResponseValidator pagedResponseValidator) {
        super(pagedResponseValidator);
    }

    public SubmissionFilterParams.SubmissionFilterParamsBuilder generateFilterParams(
            SubmissionListQueryParams submissionListQueryParams) {
        return SubmissionFilterParams.builder()
                .assignmentId(submissionListQueryParams.getAssignmentId())
                .userId(submissionListQueryParams.getUserId())
                .status(submissionListQueryParams.getStatus())
                .submittedAfter(submissionListQueryParams.getSubmittedAfter())
                .submittedBefore(submissionListQueryParams.getSubmittedBefore());
    }

    @Override
    public Map<String, String> sortFieldMappings() {
        return Map.of(
                "id", "s.id",
                "status", "s.status",
                "language", "s.language",
                "finalScore", "s.finalScore",
                "submittedAt", "s.submittedAt"
        );
    }
}
