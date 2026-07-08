package com.codeevaluation.core.service;

import com.codeevaluation.core.client.plagscan.PlagscanRestClient;
import com.codeevaluation.core.client.plagscan.dto.PlagscanResult;
import com.codeevaluation.core.client.plagscan.dto.PlagscanScanRequest;
import com.codeevaluation.core.util.PlagscanLimiter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlagscanService {

    @RestClient
    private final PlagscanRestClient plagscanRestClient;
    private final PlagscanLimiter plagscanLimiter;

    public PlagscanResult scanCpp(PlagscanScanRequest request) {
        if (!plagscanLimiter.tryAcquire()) {
            throw new WebApplicationException(
                    "Too many concurrent plagiarism scans",
                    Response.Status.TOO_MANY_REQUESTS
            );
        }

        try {
            return plagscanRestClient.scanCpp(request);
        } finally {
            plagscanLimiter.release();
        }
    }
}
