package com.codeevaluation.core.api.dto.submission;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.SubmissionStatus;
import com.codeevaluation.core.model.Submission;
import com.codeevaluation.core.model.SubmissionTestRun;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record SubmissionListItemDto(
        Long id,
        Long assignmentId,
        Long taskId,
        UserDto user,
        SubmissionStatus status,
        String code,
        BigDecimal finalScore,
        Instant submittedAt,
        Integer totalTests,
        Integer passedTests,
        Integer similarityCount
) {

    public static SubmissionListItemDto from(Submission submission) {
        SubmissionTestRun latestTestRun = submission.getTestRuns().isEmpty()
                ? null
                : submission.getTestRuns().getLast();

        return SubmissionListItemDto.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .taskId(submission.getTask().getId())
                .user(UserDto.from(submission.getUser()))
                .status(submission.getStatus())
                .code(submission.getFiles().getFirst().getContent())
                .finalScore(submission.getFinalScore())
                .submittedAt(submission.getSubmittedAt())
                .totalTests(latestTestRun == null ? null : latestTestRun.getTotalTests())
                .passedTests(latestTestRun == null ? null : latestTestRun.getPassedTests())
                .similarityCount(submission.getSimilarities().size())
                .build();
    }

    public static List<SubmissionListItemDto> from(List<Submission> submissions) {
        return submissions.stream().map(SubmissionListItemDto::from).toList();
    }
}
