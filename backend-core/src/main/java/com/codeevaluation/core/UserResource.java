package com.codeevaluation.core;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.service.UserService;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Path("/api/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class UserResource {

    private final UserService userService;

    @GET
    @Authenticated
    public UserDto getUsers(
            @QueryParam("email") String email
    ) {
        return userService.findByEmail(email);
    }
}
