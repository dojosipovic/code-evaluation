package com.codeevaluation.core.api.resource;

import com.codeevaluation.core.api.dto.auth.LoginRequestDto;
import com.codeevaluation.core.api.dto.auth.LoginResponseDto;
import com.codeevaluation.core.api.dto.auth.PlagScanTokenResponseDto;
import com.codeevaluation.core.api.dto.auth.RefreshResponseDto;
import com.codeevaluation.core.api.dto.auth.RegisterRequestDto;
import com.codeevaluation.core.api.dto.auth.TotpVerifyRequestDto;
import com.codeevaluation.core.api.dto.auth.TwoFactorTotpVerifyRequestDto;
import com.codeevaluation.core.api.dto.auth.WebAuthnFinishRequestDto;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.service.AuthService;
import com.codeevaluation.core.service.TwoFactorService;
import com.codeevaluation.core.util.CookieUtil;
import com.codeevaluation.core.util.TokenUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    TwoFactorService twoFactorService;

    @Inject
    CurrentUserProvider currentUserProvider;

    @POST
    @Path("/login")
    public Response login(LoginRequestDto req) {
        User user = authService.authenticate(req.username(), req.password());

        if (twoFactorService.hasTwoFactor(user)) {
            String token = twoFactorService.issueLoginChallenge(user);
            return Response.ok(LoginResponseDto.twoFactorRequired(
                    token,
                    twoFactorService.primaryMethod(user),
                    twoFactorService.availableMethods(user)
            )).build();
        }

        return buildAuthenticatedResponse(user);
    }

    @POST
    @Path("/2fa/totp/verify")
    public Response verifyTotp(TwoFactorTotpVerifyRequestDto req) {
        User user = twoFactorService.verifyTotpLogin(req.twoFactorToken(), req.code());
        return buildAuthenticatedResponse(user);
    }

    @POST
    @Path("/2fa/webauthn/options")
    public Response startSecondFactorWebAuthn(TwoFactorTotpVerifyRequestDto req) {
        return Response.ok(twoFactorService.startSecondFactorAuthentication(req.twoFactorToken()))
                .build();
    }

    @POST
    @Path("/2fa/webauthn/verify/{twoFactorToken}")
    public Response finishSecondFactorWebAuthn(
            @PathParam("twoFactorToken") String twoFactorToken,
            WebAuthnFinishRequestDto req
    ) {
        User user = twoFactorService.finishSecondFactorAuthentication(
                twoFactorToken,
                req.token(),
                req.responseJson()
        );
        return buildAuthenticatedResponse(user);
    }

    @POST
    @Path("/passkey/options")
    public Response startPasswordlessPasskey() {
        return Response.ok(twoFactorService.startPasswordlessAuthentication()).build();
    }

    @POST
    @Path("/passkey/verify")
    public Response finishPasswordlessPasskey(WebAuthnFinishRequestDto req) {
        User user = twoFactorService
                .finishPasswordlessAuthentication(req.token(), req.responseJson());
        return buildAuthenticatedResponse(user);
    }

    @GET
    @Path("/2fa/settings")
    @Authenticated
    public Response twoFactorSettings() {
        return Response.ok(twoFactorService.settings(currentUserProvider.getCurrentUser())).build();
    }

    @POST
    @Path("/2fa/totp/setup")
    @Authenticated
    public Response startTotpSetup() {
        return Response.ok(twoFactorService.startTotpSetup(currentUserProvider.getCurrentUser()))
                .build();
    }

    @POST
    @Path("/2fa/totp/confirm")
    @Authenticated
    public Response confirmTotpSetup(TotpVerifyRequestDto req) {
        twoFactorService.confirmTotpSetup(currentUserProvider.getCurrentUser(), req.code());
        return Response.noContent().build();
    }

    @DELETE
    @Path("/2fa/totp")
    @Authenticated
    public Response disableTotp() {
        twoFactorService.disableTotp(currentUserProvider.getCurrentUser());
        return Response.noContent().build();
    }

    @POST
    @Path("/2fa/webauthn/register/options")
    @Authenticated
    public Response startWebAuthnRegistration() {
        return Response.ok(
                twoFactorService.startWebAuthnRegistration(currentUserProvider.getCurrentUser())
                ).build();
    }

    @POST
    @Path("/2fa/webauthn/register/verify")
    @Authenticated
    public Response finishWebAuthnRegistration(WebAuthnFinishRequestDto req) {
        twoFactorService.finishWebAuthnRegistration(
                currentUserProvider.getCurrentUser(),
                req.token(),
                req.responseJson()
        );
        return Response.noContent().build();
    }

    @DELETE
    @Path("/2fa/webauthn/{credentialId}")
    @Authenticated
    public Response deleteWebAuthnCredential(@PathParam("credentialId") Long credentialId) {
        twoFactorService.deleteWebAuthnCredential(
                currentUserProvider.getCurrentUser(), credentialId
        );
        return Response.noContent().build();
    }

    private Response buildAuthenticatedResponse(User user) {
        String access = authService.issueAccessToken(user);
        var refreshIssue = authService.issueRefreshToken(user);
        var refreshTokenCookie = CookieUtil.buildRefreshCookie(refreshIssue.refreshPlain());

        return Response.ok(LoginResponseDto.authenticated(access))
                .cookie(refreshTokenCookie)
                .build();
    }

    @POST
    @Path("/register")
    public Response register(RegisterRequestDto request) {
        UserDto user = authService.register(request);

        return Response.status(Response.Status.CREATED)
                .entity(user)
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
            authService.revokeByHash(TokenUtil.sha256(refreshPlain));
        }
        return Response.noContent()
                .cookie(CookieUtil.deleteRefreshCookie())
                .build();
    }

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

    @POST
    @Path("/plagscan-token/{assignmentId}")
    @RolesAllowed({"ADMIN", "PROF"})
    public PlagScanTokenResponseDto issuePlagScanToken(
            @PathParam("assignmentId") Long assignmentId
    ) {
        return new PlagScanTokenResponseDto(authService.issuePlagScanToken(assignmentId));
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
