package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.auth.RegisterRequestDto;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.InviteStatus;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.helper.AssignmentAccessPolicy;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Invite;
import com.codeevaluation.core.model.RefreshToken;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.AssignmentRepository;
import com.codeevaluation.core.repository.InviteRepository;
import com.codeevaluation.core.repository.RefreshTokenRepository;
import com.codeevaluation.core.repository.UserRepository;
import com.codeevaluation.core.service.dto.RefreshIssue;
import com.codeevaluation.core.service.dto.RefreshResult;
import com.codeevaluation.core.util.PasswordUtil;
import com.codeevaluation.core.util.TokenUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final InviteRepository inviteRepository;
    private final AssignmentRepository assignmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public User authenticate(String username, String password) {
        User user = userRepository.findEnabledByUsername(username)
                .orElseThrow(() -> new NotAuthorizedException("Invalid credentials", "Bearer"));

        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new NotAuthorizedException("Invalid credentials", "Bearer");
        }

        return user;
    }

    @Transactional
    public UserDto register(RegisterRequestDto request) {
        String rawToken = request.token();
        String username = request.username();
        String password = request.password();
        String firstname = request.firstname();
        String lastname = request.lastname();

        String tokenHash = TokenUtil.sha256(rawToken);

        Invite invite = inviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new NotFoundException("Invite not found"));

        if (invite.getStatus() == InviteStatus.ACCEPTED) {
            throw new BadRequestException("Invite already used");
        }

        if (invite.getStatus() == InviteStatus.REVOKED) {
            throw new BadRequestException("Invite revoked");
        }

        if (invite.getStatus() == InviteStatus.EXPIRED || invite.isExpired()) {
            invite.setStatus(InviteStatus.EXPIRED);
            throw new BadRequestException("Invite expired");
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new BadRequestException("Invite invalid");
        }

        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = invite.getEmail();
        String normalizedFirstname = normalizeFirstname(firstname);
        String normalizedLastname = normalizeLastnam(lastname);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BadRequestException("User with this email already exists");
        }

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new BadRequestException("Username already taken");
        }

        validatePasswordPolicy(password);
        String passwordHash = PasswordUtil.hash(password);

        User user = userRepository.createUser(normalizedUsername, normalizedFirstname,
                normalizedLastname, normalizedEmail, passwordHash, invite.getRole());

        inviteRepository.markAccepted(invite);

        return UserDto.from(user);
    }

    private String normalizeUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new BadRequestException("Username is required");
        }

        username = username.trim();

        if (username.length() > User.USERNAME_MAX_LENGTH) {
            throw new BadRequestException(
                    String.format("Username max length is %s", User.USERNAME_MAX_LENGTH));
        }

        return username;
    }

    private String normalizeFirstname(String firstname) {
        if (StringUtils.isBlank(firstname)) {
            throw new BadRequestException("Firstname is required");
        }

        firstname = firstname.trim();

        if (firstname.length() > User.FIRSTNAME_MAX_LENGTH) {
            throw new BadRequestException(
                    String.format("Firstname max length is %s", User.FIRSTNAME_MAX_LENGTH));
        }

        return firstname;
    }

    private String normalizeLastnam(String lastname) {
        if (StringUtils.isBlank(lastname)) {
            throw new BadRequestException("Lastname is required");
        }

        lastname = lastname.trim();

        if (lastname.length() > User.LASTNAME_MAX_LENGTH) {
            throw new BadRequestException(
                    String.format("Lastname max length is %s", User.LASTNAME_MAX_LENGTH));
        }

        return lastname;
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters");
        }
    }

    public String issueAccessToken(User user) {
        return Jwt.issuer("code-evaluation")
                .subject(user.getUsername())
                .groups(Set.of(user.getRole().toString()))
                .expiresIn(Duration.ofMinutes(10))
                .sign();
    }

    public String issuePlagScanToken(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithTaskAndTests(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        User currentUser = currentUserProvider.getCurrentUser();

        if (!AssignmentAccessPolicy.canIssuePlagScanToken(assignment, currentUser)) {
            throw new ForbiddenException("You cannot issue a PlagScan token for this assignment");
        }

        return Jwt.issuer("code-evaluation")
                .subject(currentUser.getUsername())
                .groups(Set.of(Role.PLAGSCAN.toString()))
                .claim("assignmentId", assignment.getId())
                .expiresIn(Duration.ofMinutes(10))
                .sign();
    }

    public RefreshIssue issueRefreshToken(User user) {
        String refreshPlain = TokenUtil.generateToken();
        String refreshHash = TokenUtil.sha256(refreshPlain);
        Instant expiresAt = Instant.now().plus(14, ChronoUnit.DAYS);

        refreshTokenRepository.issueRefreshToken(user, expiresAt, refreshHash);

        return new RefreshIssue(refreshPlain);
    }

    public RefreshResult refresh(String refreshPlain) {
        if (refreshPlain == null || refreshPlain.isBlank()) {
            throw new NotAuthorizedException("Missing refresh token", "Bearer");
        }

        String hash = TokenUtil.sha256(refreshPlain);
        RefreshToken current = refreshTokenRepository.findActiveByHash(hash)
                .orElseThrow(() -> new NotAuthorizedException("Invalid refresh token", "Bearer"));

        String newRefreshPlain = TokenUtil.generateToken();
        String newHash = TokenUtil.sha256(newRefreshPlain);
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
