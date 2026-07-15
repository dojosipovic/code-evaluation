package com.codeevaluation.core.repository;

import com.codeevaluation.core.model.SubmissionPlagiarismRun;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class SubmissionPlagiarismRunRepository implements
        PanacheRepository<SubmissionPlagiarismRun> {

    public Optional<SubmissionPlagiarismRun> findLatestByAssignmentId(Long assignmentId) {
        return find(
                """
                    select run from SubmissionPlagiarismRun run
                    where run.assignment.id = ?1
                    order by run.createdAt desc
                """,
                assignmentId
        ).firstResultOptional();
    }
}
