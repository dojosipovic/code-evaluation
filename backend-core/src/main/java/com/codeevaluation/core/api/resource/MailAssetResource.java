package com.codeevaluation.core.api.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

@Path("/api/mail-assets")
public class MailAssetResource {

    private static final String LOGO_RESOURCE_PATH =
            "/META-INF/resources/mail-assets/logo.png";

    @GET
    @Path("/logo.png")
    @Produces("image/png")
    public Response logo() {
        InputStream logoStream = getClass().getResourceAsStream(LOGO_RESOURCE_PATH);
        if (logoStream == null) {
            throw new NotFoundException("Mail logo not found");
        }

        return Response.ok(logoStream, MediaType.valueOf("image/png"))
                .header("Cache-Control", "public, max-age=86400")
                .build();
    }
}
