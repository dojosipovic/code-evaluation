package com.codeevaluation.core.repository;

import com.codeevaluation.core.model.RefreshToken;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepository<RefreshToken> {

    public Optional<RefreshToken> findActiveByHash(String tokenHash) {
        return find(
                    """
                        tokenHash = ?1
                        and user.enabled = true
                    """, tokenHash)
                .firstResultOptional()
                .filter(RefreshToken::isActive);
    }

    @Transactional
    public void revokeByHash(String tokenHash) {
        update("revokedAt = ?1 where tokenHash = ?2 and revokedAt is null",
                Instant.now(), tokenHash);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        update("revokedAt = ?1 where user.id = ?2 and revokedAt is null",
                Instant.now(), userId);
    }

    public void deleteExpired() {
        delete("expiresAt < ?1", Instant.now());
    }

    @Transactional
    public RefreshToken issueRefreshToken(User user, Instant expiresAt, String refreshHash) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(refreshHash);
        rt.setExpiresAt(expiresAt);

        persist(rt);

        return rt;
    }

    @Transactional
    public void refresh(RefreshToken current, Instant expiresAt, String newTokenHash) {
        current = getEntityManager().merge(current);
        current.setRevokedAt(Instant.now());

        RefreshToken next = new RefreshToken();
        next.setUser(current.getUser());
        next.setTokenHash(newTokenHash);
        next.setExpiresAt(expiresAt);
        persist(next);

        current.setReplacedById(next.getId());
    }
}
