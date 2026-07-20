package com.codeevaluation.core.api.dto.plagscan;

import com.codeevaluation.core.model.SubmissionCluster;
import com.codeevaluation.core.model.SubmissionClusterMember;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.Builder;

@Builder
public record PlagScanClusterDto(
        Long id,
        Long plagiarismRunId,
        Long assignmentId,
        String assignmentName,
        BigDecimal similarity,
        Instant createdAt,
        List<PlagScanClusterMemberDto> members
) {
    public static PlagScanClusterDto from(SubmissionCluster cluster) {
        return PlagScanClusterDto.builder()
                .id(cluster.getId())
                .plagiarismRunId(cluster.getPlagiarismRun().getId())
                .assignmentId(cluster.getPlagiarismRun().getAssignment().getId())
                .assignmentName(cluster.getPlagiarismRun().getAssignment().getName())
                .similarity(cluster.getSimilarity())
                .createdAt(cluster.getCreatedAt())
                .members(cluster.getMembers().stream()
                        .sorted(Comparator.comparing(SubmissionClusterMember::getId))
                        .map(PlagScanClusterMemberDto::from)
                        .toList())
                .build();
    }

    public static List<PlagScanClusterDto> from(List<SubmissionCluster> clusters) {
        return clusters.stream().map(PlagScanClusterDto::from).toList();
    }

}
