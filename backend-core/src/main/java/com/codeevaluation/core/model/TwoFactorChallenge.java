package com.codeevaluation.core.model;

import com.codeevaluation.core.enumeration.TwoFactorChallengeType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "two_factor_challenge")
@Getter
@Setter
public class TwoFactorChallenge extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false, length = 40)
    private TwoFactorChallengeType challengeType;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "request_json", columnDefinition = "text")
    private String requestJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public boolean isActive() {
        return consumedAt == null && Instant.now().isBefore(expiresAt);
    }
}
