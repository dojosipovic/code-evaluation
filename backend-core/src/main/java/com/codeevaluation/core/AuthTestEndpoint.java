package com.codeevaluation.core;

import io.quarkus.security.Authenticated;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
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

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String accessToken) {}

    @POST
    @Path("/login")
    public LoginResponse login(LoginRequest req) {
        // TODO: validiraj iz baze + hash provjera
        if (!"dominik".equals(req.username()) || !"test".equals(req.password())) {
            throw new NotAuthorizedException("Bad credentials");
        }

        String token =
                Jwt.issuer("my-app")
                        .subject(req.username())
                        .groups(Set.of("USER"))
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
