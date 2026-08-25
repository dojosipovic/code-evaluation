package com.codeevaluation.core.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "webauthn_credential")
@Getter
@Setter
public class WebAuthnCredential extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "credential_id", nullable = false, unique = true, length = 512)
    private String credentialId;

    @Column(name = "public_key_cose", nullable = false, columnDefinition = "text")
    private String publicKeyCose;

    @Column(name = "signature_count", nullable = false)
    private Long signatureCount;

    @Column(name = "aaguid", length = 128)
    private String aaguid;

    @Column(name = "discoverable")
    private Boolean discoverable;

    @Column(name = "backup_eligible")
    private Boolean backupEligible;

    @Column(name = "backed_up")
    private Boolean backedUp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
