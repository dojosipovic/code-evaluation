package com.codeevaluation.core.api.dto.submission;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.SubmissionStatus;
import com.codeevaluation.core.model.Submission;
import com.codeevaluation.core.model.SubmissionSimilarity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
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
        Instant submittedAt,
        List<SubmissionTestRunDto> testRuns,
        List<SubmissionSimilarityDto> similarities
) {
    public static SubmissionDetailResponseDto from(
            Submission submission,
            boolean showHiddenExpectedOutputs,
            List<SubmissionSimilarity> similarities
    ) {
        return SubmissionDetailResponseDto.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .taskId(submission.getTask().getId())
                .user(UserDto.from(submission.getUser()))
                .status(submission.getStatus())
                .code(submission.getFiles().getFirst().getContent())
                .finalScore(submission.getFinalScore())
                .submittedAt(submission.getSubmittedAt())
                .testRuns(SubmissionTestRunDto.from(
                        submission.getTestRuns(),
                        showHiddenExpectedOutputs
                ))
                .similarities(SubmissionSimilarityDto.from(submission, similarities))
                .build();
    }

    @Builder
    public record SubmissionSimilarityDto(
            Long id,
            Long plagiarismRunId,
            Long matchedSubmissionId,
            UserDto matchedUser,
            BigDecimal similarityScore,
            Instant createdAt
    ) {
        public static SubmissionSimilarityDto from(
                Submission submission,
                SubmissionSimilarity similarity
        ) {
            Submission matchedSubmission = Objects.equals(
                    similarity.getSourceSubmission().getId(),
                    submission.getId()
            )
                    ? similarity.getTargetSubmission()
                    : similarity.getSourceSubmission();

            return SubmissionSimilarityDto.builder()
                    .id(similarity.getId())
                    .plagiarismRunId(similarity.getPlagiarismRun().getId())
                    .matchedSubmissionId(matchedSubmission.getId())
                    .matchedUser(UserDto.from(matchedSubmission.getUser()))
                    .similarityScore(similarity.getSimilarityScore())
                    .createdAt(similarity.getCreatedAt())
                    .build();
        }

        public static List<SubmissionSimilarityDto> from(
                Submission submission,
                List<SubmissionSimilarity> similarities
        ) {
            return similarities.stream()
                    .map(similarity -> from(submission, similarity))
                    .toList();
        }
    }
}
