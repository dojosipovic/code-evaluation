package com.codeevaluation.core.util;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.Semaphore;

@ApplicationScoped
public class SandboxLimiter {

    private final Semaphore permits = new Semaphore(1);

    public boolean tryAcquire() {
        return  permits.tryAcquire();
    }

    public void release() {
        permits.release();
    }
}
