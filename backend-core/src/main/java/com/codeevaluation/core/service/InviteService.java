package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.invite.InviteResponseDto;
import com.codeevaluation.core.api.dto.invite.InviteValidateDto;
import com.codeevaluation.core.enumeration.InviteStatus;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.Invite;
import com.codeevaluation.core.repository.InviteRepository;
import com.codeevaluation.core.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class InviteService {

    private static final int DEFAULT_EXPIRY_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;

    @Transactional
    public InviteResponseDto createInvite(String email, Role role, String username) {
        String normalizedEmail = normalizeEmail(email);

        expireOldPendingInvites(normalizedEmail);

        inviteRepository.findActivePendingByEmail(normalizedEmail)
                .ifPresent(existing -> {
                    existing.setStatus(InviteStatus.REVOKED);
                    existing.setRevokedAt(Instant.now());
                });

        userRepository.findByEmail(normalizedEmail)
                .ifPresent(user -> {
                    throw new BadRequestException("User already exists");
                });

        String rawToken = generateRawToken();
        String tokenHash = sha256(rawToken);

        Invite invite = new Invite();
        invite.setEmail(normalizedEmail);
        invite.setRole(role);
        invite.setStatus(InviteStatus.PENDING);
        invite.setTokenHash(tokenHash);
        invite.setExpiresAt(Instant.now().plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS));
        invite.setCreatedByAdminUsername(username);

        inviteRepository.persist(invite);

        return InviteResponseDto.from(invite);
    }

    public InviteValidateDto validateToken(String rawToken) {
        String tokenHash = sha256(rawToken);

        return inviteRepository.findByTokenHash(tokenHash)
                .map(invite -> {
                    if (invite.getStatus() == InviteStatus.REVOKED) {
                        return InviteValidateDto.invalid("INVITE_REVOKED");
                    }
                    if (invite.getStatus() == InviteStatus.ACCEPTED) {
                        return InviteValidateDto.invalid("INVITE_ALREADY_USED");
                    }
                    if (invite.getStatus() == InviteStatus.EXPIRED || invite.isExpired()) {
                        return InviteValidateDto.invalid("INVITE_EXPIRED");
                    }
                    if (invite.getStatus() != InviteStatus.PENDING) {
                        return InviteValidateDto.invalid("INVITE_INVALID");
                    }

                    return InviteValidateDto.valid(invite.getEmail(), invite.getRole());
                })
                .orElse(InviteValidateDto.invalid("INVITE_INVALID"));
    }

    @Transactional
    public Invite markAccepted(String rawToken) {
        String tokenHash = sha256(rawToken);

        Invite invite = inviteRepository.findValidByTokenHash(tokenHash)
                .orElseThrow(NotFoundException::new);

        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setAcceptedAt(Instant.now());

        return invite;
    }

    @Transactional
    public void revokeInvite(Long inviteId) {
        Invite invite = inviteRepository.findByIdOptional(inviteId)
                .orElseThrow(NotFoundException::new);

        if (invite.getStatus() == InviteStatus.PENDING && !invite.isExpired()) {
            invite.setStatus(InviteStatus.REVOKED);
            invite.setRevokedAt(Instant.now());
        }
    }

    @Transactional
    public int expireOldPendingInvites(String email) {
        List<Invite> expired = inviteRepository.findExpiredPendingByEmail(email);

        for (Invite invite : expired) {
            invite.setStatus(InviteStatus.EXPIRED);
        }

        return expired.size();
    }

    public long expirePendingInvites() {
        return inviteRepository.expirePendingInvites();
    }

    // ovo se treba maknuti u neki validator
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    // ovo dolje imas vec u tokenUtil
    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    public PagedResponse<InviteResponseDto> getInvites(
            int page,
            int size,
            String email,
            InviteStatus status,
            Role role,
            String createdByAdminUser,
            String sortBy,
            String sortDirection
    ) {
        validatePageParams(page, size);

        Sort sort = buildSort(sortBy, sortDirection);

        PanacheQuery<Invite> query = inviteRepository.search(
                email,
                status,
                role,
                createdByAdminUser,
                sort,
                page,
                size
        );

        List<InviteResponseDto> items = query.list()
                .stream()
                .map(InviteResponseDto::from)
                .toList();

        long totalItems = query.count();

        return new PagedResponse<>(items, page, size, totalItems);
    }

    private void validatePageParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        String safeSortDirection = (sortDirection == null || sortDirection.isBlank()) ? "desc" : sortDirection;

        String mappedField = switch (safeSortBy) {
            case "createdAt" -> "createdAt";
            case "email" -> "email";
            case "status" -> "status";
            case "role" -> "role";
            case "expiresAt" -> "expiresAt";
            default -> throw new IllegalArgumentException("Unsupported sortBy: " + safeSortBy);
        };

        return switch (safeSortDirection.toLowerCase()) {
            case "asc" -> Sort.ascending(mappedField);
            case "desc" -> Sort.descending(mappedField);
            default -> throw new IllegalArgumentException("Unsupported sortDirection: " + safeSortDirection);
        };
    }
}
