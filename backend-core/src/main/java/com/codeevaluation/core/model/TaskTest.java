package com.codeevaluation.core.model;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "task_test")
@Getter
@Setter
public class TaskTest extends PanacheEntityBase {

    public static final int INPUT_MAX_LENGTH = 200;
    public static final int OUTPUT_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = INPUT_MAX_LENGTH)
    private String input;

    @Column(length = OUTPUT_MAX_LENGTH)
    private String output;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestVisibility visibility;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @OneToMany(mappedBy = "taskTest")
    private List<SubmissionTestResult> submissionTestResults = new ArrayList<>();

}
