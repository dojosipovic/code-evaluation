package com.codeevaluation.core.service;

import com.codeevaluation.core.model.User;
import com.codeevaluation.core.model.WebAuthnCredential;
import com.codeevaluation.core.repository.UserRepository;
import com.codeevaluation.core.repository.WebAuthnCredentialRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.exception.Base64UrlException;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class WebAuthnCredentialStore implements CredentialRepository {

    private static final String USER_HANDLE_PREFIX = "user:";

    private final UserRepository userRepository;
    private final WebAuthnCredentialRepository credentialRepository;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userRepository.findEnabledByUsername(username)
                .map(credentialRepository::findByUser)
                .orElseGet(java.util.List::of)
                .stream()
                .flatMap(credential -> descriptor(credential).stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userRepository.findEnabledByUsername(username)
                .map(user -> userHandle(user.getId()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return decodeUserId(userHandle)
                .flatMap(userRepository::findByIdOptional)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .map(User::getUsername);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        Optional<Long> userId = decodeUserId(userHandle);
        if (userId.isEmpty()) {
            return Optional.empty();
        }

        return credentialRepository.findByCredentialId(credentialId.getBase64Url())
                .filter(credential -> credential.getUser().getId().equals(userId.get()))
                .map(this::toRegisteredCredential);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return credentialRepository.findByCredentialId(credentialId.getBase64Url())
                .map(this::toRegisteredCredential)
                .map(Set::of)
                .orElseGet(Set::of);
    }

    public ByteArray userHandle(Long userId) {
        return new ByteArray((USER_HANDLE_PREFIX + userId).getBytes(StandardCharsets.UTF_8));
    }

    private RegisteredCredential toRegisteredCredential(WebAuthnCredential credential) {
        try {
            return RegisteredCredential.builder()
                    .credentialId(ByteArray.fromBase64Url(credential.getCredentialId()))
                    .userHandle(userHandle(credential.getUser().getId()))
                    .publicKeyCose(ByteArray.fromBase64Url(credential.getPublicKeyCose()))
                    .signatureCount(credential.getSignatureCount())
                    .backupEligible(credential.getBackupEligible())
                    .backupState(credential.getBackedUp())
                    .build();
        } catch (Base64UrlException e) {
            throw new IllegalStateException("Stored WebAuthn credential is not valid Base64Url", e);
        }
    }

    private Optional<PublicKeyCredentialDescriptor> descriptor(WebAuthnCredential credential) {
        try {
            return Optional.of(PublicKeyCredentialDescriptor.builder()
                    .id(ByteArray.fromBase64Url(credential.getCredentialId()))
                    .build());
        } catch (Base64UrlException e) {
            return Optional.empty();
        }
    }

    private Optional<Long> decodeUserId(ByteArray userHandle) {
        String value = new String(userHandle.getBytes(), StandardCharsets.UTF_8);
        if (!value.startsWith(USER_HANDLE_PREFIX)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(value.substring(USER_HANDLE_PREFIX.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
