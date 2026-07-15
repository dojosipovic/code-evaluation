package com.codeevaluation.core.api.dto.submission;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.SubmissionStatus;
import com.codeevaluation.core.model.Submission;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record SubmissionResponseDto(
        Long id,
        Long assignmentId,
        Long taskId,
        UserDto user,
        SubmissionStatus status,
        String code,
        BigDecimal finalScore,
        Instant submittedAt
) {

    public static SubmissionResponseDto from(Submission submission) {
        return SubmissionResponseDto.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .taskId(submission.getTask().getId())
                .user(UserDto.from(submission.getUser()))
                .status(submission.getStatus())
                .code(submission.getFiles().getFirst().getContent())
                .finalScore(submission.getFinalScore())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }

    public static List<SubmissionResponseDto> from(List<Submission> submissions) {
        return submissions.stream().map(SubmissionResponseDto::from).toList();
    }
}
