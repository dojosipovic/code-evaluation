package com.codeevaluation.core.model;

import com.codeevaluation.core.enumeration.SubmissionTestRunStatus;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "submission_test_run")
@Getter
@Setter
public class SubmissionTestRun extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionTestRunStatus status;

    @Column(name = "total_tests", nullable = false)
    private Integer totalTests;

    @Column(name = "passed_tests", nullable = false)
    private Integer passedTests;

    @Column(name = "runtime_ms")
    private Long runtimeMs;

    @Column(name = "memory_bytes")
    private Long memoryBytes;

    @Column(name = "log_output")
    private String logOutput;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "submissionTestRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SubmissionTestResult> testResults = new ArrayList<>();

    public void addTestResult(SubmissionTestResult testResult) {
        testResults.add(testResult);
        testResult.setSubmissionTestRun(this);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
