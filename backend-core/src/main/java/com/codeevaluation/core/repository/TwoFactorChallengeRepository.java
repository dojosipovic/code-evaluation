package com.codeevaluation.core.repository;

import com.codeevaluation.core.enumeration.TwoFactorChallengeType;
import com.codeevaluation.core.model.TwoFactorChallenge;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.util.TokenUtil;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class TwoFactorChallengeRepository implements PanacheRepository<TwoFactorChallenge> {

    public record ChallengeIssue(String token, TwoFactorChallenge challenge) {
    }

    @Transactional
    public ChallengeIssue issue(
            User user,
            TwoFactorChallengeType type,
            Duration ttl,
            String requestJson
    ) {
        String token = TokenUtil.generateToken();
        TwoFactorChallenge challenge = new TwoFactorChallenge();
        challenge.setUser(user);
        challenge.setChallengeType(type);
        challenge.setTokenHash(TokenUtil.sha256(token));
        challenge.setRequestJson(requestJson);
        challenge.setCreatedAt(Instant.now());
        challenge.setExpiresAt(Instant.now().plus(ttl));
        persist(challenge);

        return new ChallengeIssue(token, challenge);
    }

    public Optional<TwoFactorChallenge> findActive(String token, TwoFactorChallengeType type) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return find("tokenHash = ?1 and challengeType = ?2", TokenUtil.sha256(token), type)
                .firstResultOptional()
                .filter(TwoFactorChallenge::isActive);
    }

    @Transactional
    public void consume(TwoFactorChallenge challenge) {
        challenge = getEntityManager().merge(challenge);
        challenge.setConsumedAt(Instant.now());
    }
}
