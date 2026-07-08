package com.codeevaluation.plagscan.dto;

import java.util.List;

public record BaseCode(
        List<FilePayload> files) {
}
