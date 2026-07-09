package com.codeevaluation.core.api.dto.submission;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.SubmissionStatus;
import com.codeevaluation.core.model.Submission;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
public record SubmissionDetailResponseDto(
        Long id,
        Long assignmentId,
        Long taskId,
        UserDto user,
        SubmissionStatus status,
        String code,
        BigDecimal finalScore,
        Instant submittedAt
) {
    public static SubmissionDetailResponseDto from(Submission submission) {
        return SubmissionDetailResponseDto.builder()
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
}
