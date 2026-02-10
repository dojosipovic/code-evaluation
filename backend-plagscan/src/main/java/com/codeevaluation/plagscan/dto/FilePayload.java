package com.codeevaluation.plagscan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class FilePayload {

    @NotBlank
    private String id;

    @NotBlank
    @NotNull
    private String contentBase64;
}
