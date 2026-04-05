package com.codeevaluation.core;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.service.UserService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class UserResource {

    private final UserService userService;
    private final JsonWebToken jsonWebToken;

    @GET
    @Path("/search-email")
    @Authenticated
    public UserDto getUserByEmail(
            @QueryParam("email") String email
    ) {
        return userService.findByEmail(email);
    }

    @GET
    @Path("/search-username")
    public UserDto getUserByUsername(
            @QueryParam("username") String username
    ) {
        return userService.findByUsername(username);
    }

    @GET
    @RolesAllowed("ADMIN")
    public Response getUsers(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("username") String username,
            @QueryParam("email") String email,
            @QueryParam("search") String search,
            @QueryParam("role") Role role,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("desc") String sortDirection
    ) {
        PagedResponse<UserDto> response = userService.getUsers(page, size, username, email,
                search, role, enabled, sortBy, sortDirection
        );

        return Response.ok(response).build();
    }

    @PATCH
    @Path("/{id}/enabled")
    @RolesAllowed("ADMIN")
    public Response enableUser(
            @PathParam("id") Long userId,
            Boolean enabled
    ) {
        String username = jsonWebToken.getSubject();
        userService.setEnabled(userId, enabled, username);
        return Response.noContent().build();
    }
}
