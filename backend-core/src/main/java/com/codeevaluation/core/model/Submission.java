package com.codeevaluation.core.model;

import com.codeevaluation.core.enumeration.ProgrammingLanguage;
import com.codeevaluation.core.enumeration.SubmissionStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "submission",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submission_assignment_user",
                columnNames = {"assignment_id", "user_id"}
        )
)
@Getter
@Setter
public class Submission extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProgrammingLanguage language;

    @Column(name = "final_score", precision = 10, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SubmissionFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SubmissionTestRun> testRuns = new ArrayList<>();

    @OneToMany(mappedBy = "sourceSubmission")
    @OrderBy("id ASC")
    private List<SubmissionSimilarity> similarities = new ArrayList<>();

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SubmissionClusterMember> clusterMemberships = new ArrayList<>();

    public void addFile(SubmissionFile file) {
        files.add(file);
        file.setSubmission(this);
    }

    public void addTestRun(SubmissionTestRun testRun) {
        testRuns.add(testRun);
        testRun.setSubmission(this);
    }

    public void addSimilarity(SubmissionSimilarity similarity) {
        similarities.add(similarity);
        similarity.setSourceSubmission(this);
    }

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }
}
