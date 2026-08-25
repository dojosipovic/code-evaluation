package com.codeevaluation.core.repository;

import com.codeevaluation.core.model.User;
import com.codeevaluation.core.model.WebAuthnCredential;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WebAuthnCredentialRepository implements PanacheRepository<WebAuthnCredential> {

    public List<WebAuthnCredential> findByUser(User user) {
        return list("user", user);
    }

    public Optional<WebAuthnCredential> findByCredentialId(String credentialId) {
        return find("credentialId", credentialId).firstResultOptional();
    }

    public boolean hasForUser(User user) {
        return count("user", user) > 0;
    }

    @Transactional
    public WebAuthnCredential create(
            User user,
            String credentialId,
            String publicKeyCose,
            long signatureCount,
            String aaguid,
            Boolean discoverable,
            Boolean backupEligible,
            Boolean backedUp
    ) {
        WebAuthnCredential credential = new WebAuthnCredential();
        credential.setUser(user);
        credential.setCredentialId(credentialId);
        credential.setPublicKeyCose(publicKeyCose);
        credential.setSignatureCount(signatureCount);
        credential.setAaguid(aaguid);
        credential.setDiscoverable(discoverable);
        credential.setBackupEligible(backupEligible);
        credential.setBackedUp(backedUp);
        credential.setCreatedAt(Instant.now());

        persist(credential);
        return credential;
    }

    @Transactional
    public void markUsed(String credentialId, long signatureCount, Boolean backedUp) {
        findByCredentialId(credentialId).ifPresent(credential -> {
            WebAuthnCredential managed = getEntityManager().merge(credential);
            managed.setSignatureCount(signatureCount);
            managed.setBackedUp(backedUp);
            managed.setLastUsedAt(Instant.now());
        });
    }

    @Transactional
    public void deleteForUser(User user, Long credentialId) {
        delete("user = ?1 and id = ?2", user, credentialId);
    }
}
