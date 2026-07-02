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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "submission_file",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submission_file_submission_path",
                columnNames = {"submission_id", "file_path"}
        )
)
@Getter
@Setter
public class SubmissionFile extends PanacheEntityBase {

    public static final int FILE_PATH_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "file_path", nullable = false, length = FILE_PATH_MAX_LENGTH)
    private String filePath;

    @Column(name = "content_base64", nullable = false)
    private String contentBase64;

    @Column(name = "content", nullable = false)
    private String content;

}
