package com.codeevaluation.core;

import com.codeevaluation.core.model.User;
import com.codeevaluation.core.service.AuthService;
import io.quarkus.security.Authenticated;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.Set;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthTestEndpoint {

    @Inject
    AuthService authService;

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String accessToken) {}

    @POST
    @Path("/login")
    public LoginResponse login(LoginRequest req) {

        // nije dobro da se tu vraca user nego da service vrati token
        User user = authService.authenticate(req.username(), req.password());

        String token =
                Jwt.issuer("code-evaluation")
                        .subject(user.getUsername())
                        .groups(Set.of(user.getRole().toString()))
                        .expiresIn(Duration.ofHours(1))
                        .sign();

        return new LoginResponse(token);
    }

    @GET
    @Path("/me")
    @Authenticated
    public String me() {
        return "OK";
    }

    @GET
    @RolesAllowed("ADMIN")
    public String adminOnly() {
        return "secret";
    }
}
