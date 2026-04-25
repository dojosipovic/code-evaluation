package com.codeevaluation.core.helper;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PagedResponseValidator {

    private static final int MAX_PAGE_SIZE = 100;

    public void validatePageParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
