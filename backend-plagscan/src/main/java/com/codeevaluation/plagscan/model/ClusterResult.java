package com.codeevaluation.plagscan.model;

import lombok.AllArgsConstructor;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ClusterResult {
    private int clusterId;
    private double similarity;
    private List<String> members;
}
