package com.codeevaluation.core.service;

import com.codeevaluation.core.model.RefreshToken;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.repository.RefreshTokenRepository;
import com.codeevaluation.core.repository.UserRepository;
import com.codeevaluation.core.service.dto.RefreshIssue;
import com.codeevaluation.core.service.dto.RefreshResult;
import com.codeevaluation.core.util.PasswordUtil;
import com.codeevaluation.core.util.TokenUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotAuthorizedException("Invalid credentials", "Bearer"));

        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new NotAuthorizedException("Invalid credentials", "Bearer");
        }

        return user;
    }

    public String issueAccessToken(User user) {
        return Jwt.issuer("code-evaluation")
                .subject(user.getUsername())
                .groups(Set.of(user.getRole().toString()))
                .expiresIn(Duration.ofMinutes(10))
                .sign();
    }

    public RefreshIssue issueRefreshToken(User user) {
        String refreshPlain = TokenUtil.generateRefreshToken();
        String refreshHash = TokenUtil.sha256Hex(refreshPlain);
        Instant expiresAt = Instant.now().plus(14, ChronoUnit.DAYS);

        refreshTokenRepository.issueRefreshToken(user, expiresAt, refreshHash);

        return new RefreshIssue(refreshPlain);
    }

    public RefreshResult refresh(String refreshPlain) {
        if (refreshPlain == null || refreshPlain.isBlank()) {
            throw new NotAuthorizedException("Missing refresh token", "Bearer");
        }

        String hash = TokenUtil.sha256Hex(refreshPlain);
        RefreshToken current = refreshTokenRepository.findActiveByHash(hash)
                .orElseThrow(() -> new NotAuthorizedException("Invalid refresh token", "Bearer"));

        String newRefreshPlain = TokenUtil.generateRefreshToken();
        String newHash = TokenUtil.sha256Hex(newRefreshPlain);
        Instant expiresAt = Instant.now().plus(14, ChronoUnit.DAYS);

        // ROTACIJA
        refreshTokenRepository.refresh(current, expiresAt, newHash);

        String newAccess = issueAccessToken(current.getUser());

        return new RefreshResult(newAccess, newRefreshPlain);
    }

    public void revokeByHash(String tokenHash) {
        refreshTokenRepository.revokeByHash(tokenHash);
    }

    public void logoutEverywhere(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        userOptional.ifPresent(user -> refreshTokenRepository.revokeAllForUser(user.getId()));
    }
}
