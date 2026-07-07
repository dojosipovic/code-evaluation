package com.codeevaluation.core.service;

import com.codeevaluation.core.client.plagscan.PlagscanRestClient;
import com.codeevaluation.core.client.plagscan.dto.PlagscanResult;
import com.codeevaluation.core.client.plagscan.dto.PlagscanScanRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlagscanService {

    @RestClient
    private final PlagscanRestClient plagscanRestClient;

    public PlagscanResult scanCpp(PlagscanScanRequest request) {
        return plagscanRestClient.scanCpp(request);
    }
}
