package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.plagscan.PlagScanClusterDto;
import com.codeevaluation.core.api.dto.plagscan.PlagScanClusterFilterParams;
import com.codeevaluation.core.api.query.PlagScanClusterQueryParams;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.helper.PagedSearchPlagScanClusterImpl;
import com.codeevaluation.core.model.SubmissionCluster;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.SubmissionClusterRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class PlagScanClusterService {

    private final PagedSearchPlagScanClusterImpl pagedSearchPlagScanCluster;
    private final SubmissionClusterRepository submissionClusterRepository;
    private final CurrentUserProvider currentUserProvider;

    public PagedResponse<PlagScanClusterDto> getClusters(
            PlagScanClusterQueryParams queryParams
    ) {
        PagedContext pagedContext = pagedSearchPlagScanCluster.generateFrom(queryParams);
        PlagScanClusterFilterParams filterParams =
                pagedSearchPlagScanCluster.generateFilterParams(queryParams)
                        .user(currentUserProvider.getCurrentUser())
                        .build();

        PanacheQuery<SubmissionCluster> query =
                submissionClusterRepository.findClusters(pagedContext, filterParams);
        List<SubmissionCluster> clusters = query.list();
        long totalItems = query.count();

        return new PagedResponse<>(
                PlagScanClusterDto.from(clusters),
                pagedContext.page(),
                pagedContext.size(),
                totalItems
        );
    }
}
