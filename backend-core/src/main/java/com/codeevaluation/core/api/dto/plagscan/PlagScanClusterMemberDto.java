package com.codeevaluation.core.api.dto.plagscan;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.model.SubmissionClusterMember;
import java.time.Instant;
import lombok.Builder;

@Builder
public record PlagScanClusterMemberDto(
        Long id,
        Long submissionId,
        UserDto user,
        Instant submittedAt
) {
    public static PlagScanClusterMemberDto from(SubmissionClusterMember member) {
        return PlagScanClusterMemberDto.builder()
                .id(member.getId())
                .submissionId(member.getSubmission().getId())
                .user(UserDto.from(member.getSubmission().getUser()))
                .submittedAt(member.getSubmission().getSubmittedAt())
                .build();
    }
}
