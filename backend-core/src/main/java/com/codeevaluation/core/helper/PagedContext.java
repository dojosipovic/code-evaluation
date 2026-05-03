package com.codeevaluation.core.helper;

import io.quarkus.panache.common.Sort;
import lombok.Builder;

@Builder
public record PagedContext(
        int page,
        int size,
        Sort sort,
        String search
) {}
