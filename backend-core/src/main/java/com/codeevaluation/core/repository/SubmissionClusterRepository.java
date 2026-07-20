package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.plagscan.PlagScanClusterFilterParams;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.helper.PlagScanClusterAccessPolicy;
import com.codeevaluation.core.model.SubmissionCluster;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class SubmissionClusterRepository implements PanacheRepository<SubmissionCluster> {

    public PanacheQuery<SubmissionCluster> findClusters(
            PagedContext pagedContext,
            PlagScanClusterFilterParams filterParams
    ) {
        StringBuilder query = new StringBuilder(
                """
                select distinct c from SubmissionCluster c
                join fetch c.plagiarismRun plagiarismRun
                join fetch plagiarismRun.assignment a
                join fetch a.group g
                join fetch g.owner owner
                left join c.members members
                left join members.submission submission
                left join submission.user submissionUser
                where 1=1
                """
        );
        Map<String, Object> params = new HashMap<>();
        User currentUser = filterParams.user();

        if (!PlagScanClusterAccessPolicy.canSeeAllClusters(currentUser)) {
            query.append(" and g.owner.id = :ownerId");
            params.put("ownerId", currentUser.getId());
        }

        if (filterParams.assignmentId() != null) {
            query.append(" and a.id = :assignmentId");
            params.put("assignmentId", filterParams.assignmentId());
        }

        if (!StringUtils.isBlank(pagedContext.search())) {
            query.append(
                    """
                    and (
                            lower(a.name) like :search
                            or lower(submissionUser.username) like :search
                            or lower(submissionUser.firstname) like :search
                            or lower(submissionUser.lastname) like :search
                            or lower(concat(submissionUser.firstname, ' ', submissionUser.lastname)) like :search
                        )
                    """);
            params.put("search", "%" + pagedContext.search().toLowerCase().trim() + "%");
        }

        return find(query.toString(), pagedContext.sort(), params)
                .page(Page.of(pagedContext.page(), pagedContext.size()));
    }
}
