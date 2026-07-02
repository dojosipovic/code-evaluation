package com.codeevaluation.core.api.dto.assignment;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.SubmissionStatus;
import com.codeevaluation.core.model.Submission;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
public record AssignmentSubmitResponseDto(
        Long id,
        Long assignmentId,
        UserDto submitter,
        SubmissionStatus status,
        BigDecimal finalScore,
        Instant submittedAt,
        String code
) {
    public static AssignmentSubmitResponseDto from(Submission submission) {
        return AssignmentSubmitResponseDto.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .submitter(UserDto.from(submission.getUser()))
                .status(submission.getStatus())
                .finalScore(submission.getFinalScore())
                .submittedAt(submission.getSubmittedAt())
                .code(submission.getFiles().getFirst().getContent())
                .build();
    }
}
