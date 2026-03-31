package com.codeevaluation.core.repository;

import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public Optional<User> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public Optional<User> findEnabledByUsername(String username) {
        return find("username = ?1 and enabled = true", username)
                .firstResultOptional();
    }

    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    @Transactional
    public User createUser(String username, String email, String passwordHash, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setEnabled(true);

        persist(user);
        return user;
    }
}
