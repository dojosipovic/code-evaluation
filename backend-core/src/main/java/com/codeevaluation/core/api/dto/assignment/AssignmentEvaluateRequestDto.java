package com.codeevaluation.core.api.dto.assignment;

import com.codeevaluation.core.api.dto.submission.SubmissionGradeRequestDto;
import java.util.List;

public record AssignmentEvaluateRequestDto(
        List<SubmissionGradeRequestDto> submissions
) {
}
