package com.codeevaluation.core.repository;

import com.codeevaluation.core.enumeration.InviteStatus;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.Invite;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Transactional
    public long expirePendingInvites() {
        return update(
                "status = ?1 where status = ?2 and expiresAt <= ?3",
                InviteStatus.EXPIRED,
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

    public PanacheQuery<Invite> search(
            String email,
            InviteStatus status,
            Role role,
            String createdByAdminUser,
            Sort sort,
            int page,
            int size
    ) {
        StringBuilder query = new StringBuilder("from Invite i where 1=1");
        Map<String, Object> params = new HashMap<>();

        if (email != null && !email.isBlank()) {
            query.append(" and i.email LIKE :email");
            params.put("email", "%" + normalizeEmail(email) + "%");
        }

        if (status != null) {
            query.append(" and i.status = :status");
            params.put("status", status);
        }

        if (role != null) {
            query.append(" and i.role = :role");
            params.put("role", role);
        }

        if (createdByAdminUser != null && !createdByAdminUser.isBlank()) {
            query.append(" and i.createdByAdminUsername = :createdByAdminUsername");
            params.put("createdByAdminUsername", createdByAdminUser);
        }

        return find(query.toString(), sort, params).page(Page.of(page, size));
    }
}
