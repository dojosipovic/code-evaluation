package com.codeevaluation.core.exception.mapper;

import com.codeevaluation.core.error.ApiError;
import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Provider
@Priority(1)
@Slf4j
public class WebAppExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException e) {
        log.warn("Exception: {}", e.getMessage(), e);

        int status = e.getResponse().getStatus();
        String reason = Response.Status.fromStatusCode(status) != null
                ? Response.Status.fromStatusCode(status).getReasonPhrase()
                : "Error";

        ApiError body = new ApiError(
                Instant.now(),
                status,
                reason,
                safeMessage(e.getMessage()),
                uriInfo.getPath()
        );

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private String safeMessage(String msg) {
        return (msg == null || msg.isBlank()) ? "Request failed" : msg;
    }
}
