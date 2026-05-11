package com.codeevaluation.core.helper;

import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class PagedSearchGroupImpl extends PagedSearchHelper {

    public PagedSearchGroupImpl(PagedResponseValidator pagedResponseValidator) {
        super(pagedResponseValidator);
    }

    @Override
    public Map<String, String> sortFieldMappings() {
        return Map.of(
                "id", "g.id",
                "name", "g.name",
                "createdAt", "g.createdAt",
                "owner", "g.owner.lastname",
                "memberCount", "count(distinct gm.id)"
        );
    }
}
