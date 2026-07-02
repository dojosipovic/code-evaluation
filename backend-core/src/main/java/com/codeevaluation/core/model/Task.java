package com.codeevaluation.core.model;

import com.codeevaluation.core.enumeration.TaskStatus;
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
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "task")
@Getter
@Setter
public class Task extends PanacheEntityBase {

    public static final int TITLE_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "starter_code")
    private String starterCode;

    @Column(nullable = false, name = "include_starter_code")
    private Boolean includeStarterCode;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private Boolean shared;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<TaskTest> tests = new ArrayList<>();

    @OneToMany(mappedBy = "task")
    @OrderBy("id ASC")
    private List<Submission> submissions = new ArrayList<>();

    @OneToMany(mappedBy = "task")
    @OrderBy("id ASC")
    private List<SubmissionPlagiarismRun> plagiarismRuns = new ArrayList<>();

    public void addTest(TaskTest test) {
        tests.add(test);
        test.setTask(this);
    }

    public void removeTest(TaskTest test) {
        tests.remove(test);
        test.setTask(null);
    }

    public void removeAllTests() {
        tests.forEach(test -> test.setTask(null));
        tests.clear();
    }

    public boolean isOwner(String username) {
        return Objects.equals(user.getUsername(), username);
    }

    public boolean isTaskShared() {
        return Boolean.TRUE.equals(shared);
    }

    public boolean isPublished() {
        return status == TaskStatus.PUBLISHED;
    }
}
