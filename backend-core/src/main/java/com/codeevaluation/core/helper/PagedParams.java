package com.codeevaluation.core.helper;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class PagedParams {

    @QueryParam("page")
    @DefaultValue("0")
    private int page;

    @QueryParam("size")
    @DefaultValue("20")
    private int size;

    @QueryParam("search")
    private String search;

    @QueryParam("sortBy")
    @DefaultValue("id")
    private String sortBy;

    @QueryParam("sortDir")
    @DefaultValue("desc")
    private String sortDir;

}
