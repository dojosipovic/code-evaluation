package com.codeevaluation.core.repository;

import com.codeevaluation.core.enumeration.InviteStatus;
import com.codeevaluation.core.model.Invite;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InviteRepository implements PanacheRepository<Invite> {

    public Optional<Invite> findActivePendingByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return find(
                "email = ?1 and status = ?2 and expiresAt > ?3",
                normalizedEmail,
                InviteStatus.PENDING,
                Instant.now()
        ).firstResultOptional();
    }

    public List<Invite> findExpiredPendingByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return list(
                "email = ?1 and status = ?2 and expiresAt <= ?3",
                normalizedEmail,
                InviteStatus.PENDING,
                Instant.now()
        );
    }

    public Optional<Invite> findValidByTokenHash(String tokenHash) {
        return find(
                "tokenHash = ?1 and status = ?2 and expiresAt > ?3",
                tokenHash,
                InviteStatus.PENDING,
                Instant.now()
        ).firstResultOptional();
    }

    public Optional<Invite> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
