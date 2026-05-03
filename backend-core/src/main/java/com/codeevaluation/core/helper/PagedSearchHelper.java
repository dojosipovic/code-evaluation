package com.codeevaluation.core.helper;

import io.quarkus.panache.common.Sort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public abstract class PagedSearchHelper {

    private final PagedResponseValidator pagedResponseValidator;

    public abstract Map<String, String> sortFieldMappings();

    protected String defaultSortField() {
        return "id";
    }

    protected String defaultSortDir() {
        return "desc";
    }

    public PagedContext generateFrom(PagedParams pagedParams) {
        pagedResponseValidator.validatePageParams(pagedParams.getPage(), pagedParams.getSize());
        return PagedContext.builder()
                .search(pagedParams.getSearch())
                .page(pagedParams.getPage())
                .size(pagedParams.getSize())
                .sort(buildSort(pagedParams.getSortBy(), pagedParams.getSortDir()))
                .build();
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        String safeSortBy = (StringUtils.isBlank(sortBy)) ? defaultSortField() : sortBy;
        String safeSortDirection = (StringUtils.isBlank(sortDirection))
                ? defaultSortDir()
                : sortDirection;

        String sortPath = sortFieldMappings().get(safeSortBy);

        if (sortPath == null) {
            throw new IllegalArgumentException("Unsupported sortBy: " + safeSortBy);
        }

        return switch (safeSortDirection.toLowerCase()) {
            case "asc" -> Sort.ascending(sortPath);
            case "desc" -> Sort.descending(sortPath);
            default -> throw new IllegalArgumentException(
                    "Unsupported sortDirection: " + safeSortDirection);
        };
    }
}
