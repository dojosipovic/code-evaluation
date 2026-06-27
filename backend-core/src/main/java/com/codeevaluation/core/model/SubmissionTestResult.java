package com.codeevaluation.core.model;

import com.codeevaluation.core.enumeration.TestResult;
import com.codeevaluation.core.enumeration.TestVisibility;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "submission_test_result")
@Getter
@Setter
public class SubmissionTestResult extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_test_run_id", nullable = false)
    private SubmissionTestRun submissionTestRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_test_id")
    private TaskTest taskTest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TestResult result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TestVisibility visibility;

    @Column(name = "test_input")
    private String testInput;

    @Column(name = "expected_output")
    private String expectedOutput;

    @Column(name = "actual_output")
    private String actualOutput;

    @Column(name = "runtime_ms")
    private Long runtimeMs;

    @Column(name = "error_output")
    private String errorOutput;
}
