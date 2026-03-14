package com.codeevaluation.core.model;

import com.codeevaluation.core.enumeration.InviteStatus;
import com.codeevaluation.core.enumeration.Role;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invite")
@Getter
@Setter
public class Invite extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InviteStatus status;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_admin_username", nullable = false, length = 100)
    private String createdByAdminUsername;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = InviteStatus.PENDING;
        }
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isOpen() {
        return status == InviteStatus.PENDING && !isExpired();
    }
}
