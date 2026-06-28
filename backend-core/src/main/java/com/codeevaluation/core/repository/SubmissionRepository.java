package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.assignment.AssignmentSubmitRequestDto;
import com.codeevaluation.core.enumeration.ProgrammingLanguage;
import com.codeevaluation.core.enumeration.SubmissionStatus;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Submission;
import com.codeevaluation.core.model.SubmissionFile;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;

@ApplicationScoped
public class SubmissionRepository implements PanacheRepository<Submission> {

    private static final String DEFAULT_FILE_PATH = "src/main.cpp";

    public Optional<Submission> findByUserIdAndAssignmentId(Long userId, Long assignmentId) {
        return find(
                "user.id = ?1 and assignment.id = ?2",
                userId,
                assignmentId
        ).firstResultOptional();
    }

    @Transactional
    public Submission createOrUpdate(
            AssignmentSubmitRequestDto req,
            Assignment assignment,
            User user,
            String codeBase64
    ) {
        return findByUserIdAndAssignmentId(user.getId(), assignment.getId())
                .map(submission -> update(submission, assignment, codeBase64, req.code()))
                .orElseGet(() -> create(req, assignment, user, codeBase64));
    }

    @Transactional
    public Submission create(
            AssignmentSubmitRequestDto req,
            Assignment assignment,
            User user,
            String codeBase64
    ) {
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setTask(assignment.getTask());
        submission.setUser(user);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setLanguage(ProgrammingLanguage.CPP);
        submission.addFile(buildFile(req.code(), codeBase64));

        persist(submission);

        return submission;
    }

    @Transactional
    public Submission update(
            Submission submission,
            Assignment assignment,
            String codeBase64,
            String code
    ) {
        submission.setAssignment(assignment);
        submission.setTask(assignment.getTask());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setLanguage(ProgrammingLanguage.CPP);
        submission.setFinalScore(null);

        SubmissionFile file = submission.getFiles().stream()
                .filter(existingFile -> DEFAULT_FILE_PATH.equals(existingFile.getFilePath()))
                .findFirst()
                .orElseGet(() -> {
                    SubmissionFile newFile = new SubmissionFile();
                    newFile.setFilePath(DEFAULT_FILE_PATH);
                    submission.addFile(newFile);
                    return newFile;
                });

        file.setContent(code);
        file.setContentBase64(codeBase64);

        return submission;
    }

    private SubmissionFile buildFile(String code, String codeBase64) {
        SubmissionFile file = new SubmissionFile();
        file.setFilePath(DEFAULT_FILE_PATH);
        file.setContent(code);
        file.setContentBase64(codeBase64);
        return file;
    }
}
