package com.codeevaluation.core;

import com.codeevaluation.core.api.dto.auth.LoginRequestDto;
import com.codeevaluation.core.api.dto.auth.LoginResponseDto;
import com.codeevaluation.core.api.dto.auth.RefreshResponseDto;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.service.AuthService;
import com.codeevaluation.core.util.CookieUtil;
import com.codeevaluation.core.util.TokenUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequestDto req) {
        User user = authService.authenticate(req.username(), req.password());

        String access = authService.issueAccessToken(user);
        var refreshIssue = authService.issueRefreshToken(user);
        var refreshTokenCookie = CookieUtil.buildRefreshCookie(refreshIssue.refreshPlain());

        return Response.ok(new LoginResponseDto(access))
                .cookie(refreshTokenCookie)
                .build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam("refresh_token") String refreshPlain) {
        var res = authService.refresh(refreshPlain);
        var refreshTokenCookie = CookieUtil.buildRefreshCookie(res.refreshPlain());

        return Response.ok(new RefreshResponseDto(res.accessToken()))
                .cookie(refreshTokenCookie)
                .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam("refresh_token") String refreshPlain) {
        if (refreshPlain != null && !refreshPlain.isBlank()) {
            authService.revokeByHash(TokenUtil.sha256Hex(refreshPlain));
        }
        return Response.noContent()
                .cookie(CookieUtil.deleteRefreshCookie())
                .build();
    }

    // Optional: logout from everywhere (requires valid access JWT)
    @POST
    @Path("/logout-everywhere")
    @Authenticated
    public Response logoutEverywhere(@Context SecurityContext ctx) {
        String username = ctx.getUserPrincipal().getName();
        authService.logoutEverywhere(username);

        return Response.noContent()
                .cookie(CookieUtil.deleteRefreshCookie())
                .build();
    }

    @GET
    @Path("/me")
    @Authenticated
    public String me() {
        return "OK";
    }

    @GET
    @Path("/admin")
    @RolesAllowed("ADMIN")
    public String adminOnly() {
        return "secret";
    }
}
