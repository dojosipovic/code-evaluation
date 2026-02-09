package com.codeevaluation.plagscan.model;

import lombok.AllArgsConstructor;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PlagResult {

    private String runId;
    private Double minSimilarity;

    private List<PairResult> pairs;
    private List<ClusterResult> clusters;

    private String fileBase64;
}
