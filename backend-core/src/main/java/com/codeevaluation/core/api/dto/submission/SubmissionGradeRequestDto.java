package com.codeevaluation.core.api.dto.submission;

import java.math.BigDecimal;

public record SubmissionGradeRequestDto(
        Long submissionId,
        BigDecimal finalGrade
) {
}
