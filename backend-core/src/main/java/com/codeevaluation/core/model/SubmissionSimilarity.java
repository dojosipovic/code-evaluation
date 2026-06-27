package com.codeevaluation.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "submission_similarity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submission_similarity_run_submission_pair",
                columnNames = {"plagiarism_run_id", "source_submission_id", "target_submission_id"}
        )
)
@Getter
@Setter
public class SubmissionSimilarity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plagiarism_run_id", nullable = false)
    private SubmissionPlagiarismRun plagiarismRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_submission_id", nullable = false)
    private Submission sourceSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_submission_id", nullable = false)
    private Submission targetSubmission;

    @Column(name = "similarity_score", nullable = false, precision = 12, scale = 11)
    private BigDecimal similarityScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
