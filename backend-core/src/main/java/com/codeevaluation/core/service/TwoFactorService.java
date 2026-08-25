package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.auth.TotpSetupResponseDto;
import com.codeevaluation.core.api.dto.auth.TwoFactorSettingsDto;
import com.codeevaluation.core.api.dto.auth.WebAuthnCredentialDto;
import com.codeevaluation.core.api.dto.auth.WebAuthnOptionsResponseDto;
import com.codeevaluation.core.enumeration.TwoFactorChallengeType;
import com.codeevaluation.core.model.TotpCredential;
import com.codeevaluation.core.model.TwoFactorChallenge;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.repository.TotpCredentialRepository;
import com.codeevaluation.core.repository.TwoFactorChallengeRepository;
import com.codeevaluation.core.repository.UserRepository;
import com.codeevaluation.core.repository.WebAuthnCredentialRepository;
import com.codeevaluation.core.util.TotpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@RequiredArgsConstructor
public class TwoFactorService {

    private static final Logger LOG = Logger.getLogger(TwoFactorService.class);

    private static final Duration LOGIN_CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final Duration WEBAUTHN_CHALLENGE_TTL = Duration.ofMinutes(5);

    private final TotpCredentialRepository totpCredentialRepository;
    private final WebAuthnCredentialRepository webAuthnCredentialRepository;
    private final TwoFactorChallengeRepository challengeRepository;
    private final WebAuthnCredentialStore webAuthnCredentialStore;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @ConfigProperty(name = "security.totp.issuer", defaultValue = "Code Evaluation")
    String totpIssuer;

    @ConfigProperty(name = "security.webauthn.rp-id", defaultValue = "localhost")
    String rpId;

    @ConfigProperty(name = "security.webauthn.rp-name", defaultValue = "Code Evaluation")
    String rpName;

    @ConfigProperty(name = "security.webauthn.origin", defaultValue = "http://localhost:4200")
    String origin;

    public boolean hasTwoFactor(User user) {
        return totpCredentialRepository.hasConfirmed(user) || webAuthnCredentialRepository.hasForUser(user);
    }

    public List<String> availableMethods(User user) {
        List<String> methods = new ArrayList<>();
        if (totpCredentialRepository.hasConfirmed(user)) {
            methods.add("totp");
        }
        if (webAuthnCredentialRepository.hasForUser(user)) {
            methods.add("webauthn");
        }
        return methods;
    }

    public String primaryMethod(User user) {
        if (totpCredentialRepository.hasConfirmed(user)) {
            return "totp";
        }
        if (webAuthnCredentialRepository.hasForUser(user)) {
            return "webauthn";
        }
        return null;
    }

    public String issueLoginChallenge(User user) {
        return challengeRepository.issue(
                user,
                TwoFactorChallengeType.LOGIN,
                LOGIN_CHALLENGE_TTL,
                null
        ).token();
    }

    public TwoFactorSettingsDto settings(User user) {
        return new TwoFactorSettingsDto(
                totpCredentialRepository.hasConfirmed(user),
                webAuthnCredentialRepository.hasForUser(user),
                webAuthnCredentialRepository.findByUser(user).stream()
                        .map(WebAuthnCredentialDto::from)
                        .toList()
        );
    }

    @Transactional
    public TotpSetupResponseDto startTotpSetup(User user) {
        String secret = TotpUtil.generateSecret();
        TotpCredential credential = totpCredentialRepository.upsertUnconfirmed(user, secret);
        String otpauthUrl = TotpUtil.otpauthUrl(totpIssuer, user.getUsername(), credential.getSecret());
        return new TotpSetupResponseDto(credential.getSecret(), otpauthUrl);
    }

    @Transactional
    public void confirmTotpSetup(User user, String code) {
        TotpCredential credential = totpCredentialRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("TOTP setup was not started"));
        if (!TotpUtil.verify(credential.getSecret(), code)) {
            throw new BadRequestException("Invalid TOTP code");
        }
        totpCredentialRepository.confirm(credential);
    }

    @Transactional
    public User verifyTotpLogin(String twoFactorToken, String code) {
        TwoFactorChallenge loginChallenge = challengeRepository
                .findActive(twoFactorToken, TwoFactorChallengeType.LOGIN)
                .orElseThrow(() -> new NotAuthorizedException("Invalid two factor token", "Bearer"));
        User user = loginChallenge.getUser();
        TotpCredential credential = totpCredentialRepository.findConfirmedByUser(user)
                .orElseThrow(() -> new BadRequestException("TOTP is not enabled"));

        if (!TotpUtil.verify(credential.getSecret(), code)) {
            throw new NotAuthorizedException("Invalid TOTP code", "Bearer");
        }

        challengeRepository.consume(loginChallenge);
        return authenticatedUser(user);
    }

    @Transactional
    public void disableTotp(User user) {
        totpCredentialRepository.deleteForUser(user);
    }

    public WebAuthnOptionsResponseDto startWebAuthnRegistration(User user) {
        PublicKeyCredentialCreationOptions request = relyingParty().startRegistration(
                StartRegistrationOptions.builder()
                        .user(UserIdentity.builder()
                                .name(user.getUsername())
                                .displayName(user.getFirstname() + " " + user.getLastname())
                                .id(webAuthnCredentialStore.userHandle(user.getId()))
                                .build())
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                .residentKey(ResidentKeyRequirement.PREFERRED)
                                .userVerification(UserVerificationRequirement.PREFERRED)
                                .build())
                        .build()
        );

        try {
            String token = challengeRepository.issue(
                    user,
                    TwoFactorChallengeType.WEBAUTHN_REGISTRATION,
                    WEBAUTHN_CHALLENGE_TTL,
                    request.toJson()
            ).token();

            return new WebAuthnOptionsResponseDto(token, request.toJson());
        } catch (Exception e) {
            throw new BadRequestException("Could not start WebAuthn registration");
        }
    }

    @Transactional
    public void finishWebAuthnRegistration(User user, String token, String responseJson) {
        TwoFactorChallenge challenge = challengeRepository
                .findActive(token, TwoFactorChallengeType.WEBAUTHN_REGISTRATION)
                .filter(existing -> existing.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotAuthorizedException("Invalid WebAuthn token", "Bearer"));

        try {
            RegistrationResult result = relyingParty().finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(PublicKeyCredentialCreationOptions.fromJson(challenge.getRequestJson()))
                            .response(PublicKeyCredential.parseRegistrationResponseJson(responseJson))
                            .build()
            );

            webAuthnCredentialRepository.create(
                    user,
                    result.getKeyId().getId().getBase64Url(),
                    result.getPublicKeyCose().getBase64Url(),
                    result.getSignatureCount(),
                    result.getAaguid().getBase64Url(),
                    result.isDiscoverable().orElse(null),
                    result.isBackupEligible(),
                    result.isBackedUp()
            );
            challengeRepository.consume(challenge);
        } catch (Exception e) {
            LOG.warn("WebAuthn registration failed", e);
            throw new BadRequestException("WebAuthn registration failed");
        }
    }

    public WebAuthnOptionsResponseDto startPasswordlessAuthentication() {
        AssertionRequest request = relyingParty().startAssertion(StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build());

        try {
            String token = challengeRepository.issue(
                    null,
                    TwoFactorChallengeType.WEBAUTHN_PASSWORDLESS,
                    WEBAUTHN_CHALLENGE_TTL,
                    request.toJson()
            ).token();

            return new WebAuthnOptionsResponseDto(token, requestOptionsJson(request));
        } catch (Exception e) {
            throw new BadRequestException("Could not start WebAuthn authentication");
        }
    }

    @Transactional
    public WebAuthnOptionsResponseDto startSecondFactorAuthentication(String twoFactorToken) {
        TwoFactorChallenge loginChallenge = challengeRepository
                .findActive(twoFactorToken, TwoFactorChallengeType.LOGIN)
                .orElseThrow(() -> new NotAuthorizedException("Invalid two factor token", "Bearer"));

        AssertionRequest request = relyingParty().startAssertion(StartAssertionOptions.builder()
                .username(loginChallenge.getUser().getUsername())
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build());

        try {
            String token = challengeRepository.issue(
                    loginChallenge.getUser(),
                    TwoFactorChallengeType.WEBAUTHN_SECOND_FACTOR,
                    WEBAUTHN_CHALLENGE_TTL,
                    request.toJson()
            ).token();

            return new WebAuthnOptionsResponseDto(token, requestOptionsJson(request));
        } catch (Exception e) {
            throw new BadRequestException("Could not start WebAuthn authentication");
        }
    }

    @Transactional
    public User finishPasswordlessAuthentication(String token, String responseJson) {
        return finishWebAuthnAuthentication(
                token,
                responseJson,
                TwoFactorChallengeType.WEBAUTHN_PASSWORDLESS
        );
    }

    @Transactional
    public User finishSecondFactorAuthentication(String twoFactorToken, String token, String responseJson) {
        User user = finishWebAuthnAuthentication(
                token,
                responseJson,
                TwoFactorChallengeType.WEBAUTHN_SECOND_FACTOR
        );

        TwoFactorChallenge loginChallenge = challengeRepository
                .findActive(twoFactorToken, TwoFactorChallengeType.LOGIN)
                .filter(existing -> existing.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotAuthorizedException("Invalid two factor token", "Bearer"));
        challengeRepository.consume(loginChallenge);
        return authenticatedUser(user);
    }

    @Transactional
    public void deleteWebAuthnCredential(User user, Long credentialId) {
        if (credentialId == null) {
            throw new NotFoundException("Credential not found");
        }
        webAuthnCredentialRepository.deleteForUser(user, credentialId);
    }

    private User finishWebAuthnAuthentication(
            String token,
            String responseJson,
            TwoFactorChallengeType challengeType
    ) {
        TwoFactorChallenge challenge = challengeRepository
                .findActive(token, challengeType)
                .orElseThrow(() -> new NotAuthorizedException("Invalid WebAuthn token", "Bearer"));

        try {
            AssertionResult result = relyingParty().finishAssertion(FinishAssertionOptions.builder()
                    .request(AssertionRequest.fromJson(challenge.getRequestJson()))
                    .response(PublicKeyCredential.parseAssertionResponseJson(responseJson))
                    .build());

            User user = userRepository.findEnabledByUsername(result.getUsername())
                    .orElseThrow(() -> new NotAuthorizedException("Invalid WebAuthn user", "Bearer"));
            webAuthnCredentialRepository.markUsed(
                    result.getCredentialId().getBase64Url(),
                    result.getSignatureCount(),
                    result.isBackedUp()
            );
            challengeRepository.consume(challenge);
            return authenticatedUser(user);
        } catch (Exception e) {
            throw new NotAuthorizedException("WebAuthn authentication failed", "Bearer");
        }
    }

    private User authenticatedUser(User user) {
        User initialized = userRepository.findByIdOptional(user.getId())
                .filter(existing -> Boolean.TRUE.equals(existing.getEnabled()))
                .orElseThrow(() -> new NotAuthorizedException("Invalid authenticated user", "Bearer"));
        initialized.getUsername();
        initialized.getRole();
        return initialized;
    }

    private String requestOptionsJson(AssertionRequest request) {
        try {
            return objectMapper.readTree(request.toJson())
                    .get("publicKeyCredentialRequestOptions")
                    .toString();
        } catch (Exception e) {
            throw new BadRequestException("Could not serialize WebAuthn authentication options");
        }
    }

    private RelyingParty relyingParty() {
        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(rpId)
                        .name(rpName)
                        .build())
                .credentialRepository(webAuthnCredentialStore)
                .origins(Set.of(origin))
                .allowOriginPort(true)
                .build();
    }
}
