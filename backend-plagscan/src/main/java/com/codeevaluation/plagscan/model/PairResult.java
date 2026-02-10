package com.codeevaluation.plagscan.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PairResult {
    private String studentA;
    private String studentB;
    private double similarity;
}
