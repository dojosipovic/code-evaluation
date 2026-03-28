package com.codeevaluation.core;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.invite.InviteCreateDto;
import com.codeevaluation.core.api.dto.invite.InviteResponseDto;
import com.codeevaluation.core.api.dto.invite.InviteValidateDto;
import com.codeevaluation.core.enumeration.InviteStatus;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.service.InviteService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/invites")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class InviteResource {

    private final InviteService inviteService;
    private final JsonWebToken jsonWebToken;

    @POST
    @APIResponse(
            responseCode = "201",
            description = "Invite created",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = InviteResponseDto.class)
            )
    )
    @RolesAllowed("ADMIN")
    public Response createInvite(InviteCreateDto request) {
        String username = jsonWebToken.getSubject();

        InviteResponseDto invite = inviteService.createInvite(
                request.email(),
                request.role(),
                username
        );

        return Response.created(URI.create("/invites/" + invite.getId()))
                .entity(invite)
                .build();
    }

    @GET
    @Path("/validate")
    public Response validateInvite(@QueryParam("token") String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Token is required");
        }

        InviteValidateDto response = inviteService.validateToken(token);
        return Response.ok(response).build();
    }

    @POST
    @Path("/{id}/revoke")
    @RolesAllowed("ADMIN")
    public Response revokeInvite(@PathParam("id") Long inviteId) {
        inviteService.revokeInvite(inviteId);
        return Response.noContent().build();
    }

    @GET
    @RolesAllowed("ADMIN")
    public Response getInvites(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("email") String email,
            @QueryParam("status") InviteStatus status,
            @QueryParam("role") Role role,
            @QueryParam("createdByAdminId") String createdByAdminUser,
            @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("desc") String sortDirection
    ) {
        PagedResponse<InviteResponseDto> response = inviteService.getInvites(
                page,
                size,
                email,
                status,
                role,
                createdByAdminUser,
                sortBy,
                sortDirection
        );

        return Response.ok(response).build();
    }
}
