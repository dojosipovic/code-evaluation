package com.codeevaluation.core.helper;

import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class PagedSearchGroupMemberImpl extends PagedSearchHelper {

    public PagedSearchGroupMemberImpl(PagedResponseValidator pagedResponseValidator) {
        super(pagedResponseValidator);
    }

    @Override
    public Map<String, String> sortFieldMappings() {
        return Map.of(
                "id", "user.id"
        );
    }
}
