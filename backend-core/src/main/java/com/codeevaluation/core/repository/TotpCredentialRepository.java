package com.codeevaluation.core.repository;

import com.codeevaluation.core.model.TotpCredential;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class TotpCredentialRepository implements PanacheRepository<TotpCredential> {

    public Optional<TotpCredential> findByUser(User user) {
        return find("user", user).firstResultOptional();
    }

    public Optional<TotpCredential> findConfirmedByUser(User user) {
        return find("user = ?1 and confirmedAt is not null", user).firstResultOptional();
    }

    public boolean hasConfirmed(User user) {
        return count("user = ?1 and confirmedAt is not null", user) > 0;
    }

    @Transactional
    public TotpCredential upsertUnconfirmed(User user, String secret) {
        TotpCredential credential = findByUser(user).orElseGet(TotpCredential::new);
        credential.setUser(user);
        credential.setSecret(secret);
        credential.setConfirmedAt(null);
        credential.setCreatedAt(Instant.now());

        if (credential.getId() == null) {
            persist(credential);
        }

        return credential;
    }

    @Transactional
    public void confirm(TotpCredential credential) {
        credential = getEntityManager().merge(credential);
        credential.setConfirmedAt(Instant.now());
    }

    @Transactional
    public void deleteForUser(User user) {
        delete("user", user);
    }
}
