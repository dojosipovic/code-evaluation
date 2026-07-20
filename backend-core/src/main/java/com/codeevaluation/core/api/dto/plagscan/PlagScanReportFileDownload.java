package com.codeevaluation.core.api.dto.plagscan;

public record PlagScanReportFileDownload(
        byte[] bytes,
        String filename
) {
}
