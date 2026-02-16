package com.codeevaluation.core.exception.mapper;

import com.codeevaluation.core.error.ApiError;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;

@Provider
@Priority(2)
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable throwable) {
        int status = 500;

        ApiError body = new ApiError(
                Instant.now(),
                status,
                "Internal Server Error",
                safeMessage(throwable.getMessage()),
                uriInfo.getPath()
        );

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private String safeMessage(String msg) {
        return (msg == null || msg.isBlank()) ? "Unexpected error" : msg;
    }
}
