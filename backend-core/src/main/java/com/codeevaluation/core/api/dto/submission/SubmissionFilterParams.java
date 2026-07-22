package com.codeevaluation.core.api.dto.submission;

import com.codeevaluation.core.enumeration.SubmissionStatus;
import com.codeevaluation.core.model.User;
import java.time.Instant;
import lombok.Builder;

@Builder
public record SubmissionFilterParams(
        Long assignmentId,
        Long userId,
        SubmissionStatus status,
        Instant submittedAfter,
        Instant submittedBefore,
        User user
) {}
