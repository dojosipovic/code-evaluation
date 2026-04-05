package com.codeevaluation.core.repository;

import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@RequiredArgsConstructor
public class UserRepository implements PanacheRepository<User> {

    private final EntityManager em;

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
    public User createUser(String username, String firstname, String lastname, String email,
                           String passwordHash, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setEnabled(true);

        persist(user);
        return user;
    }

    public PanacheQuery<User> search(
            String username,
            String email,
            String search,
            Role role,
            Boolean enabled,
            Sort sort,
            int page,
            int size
    ) {
        StringBuilder query = new StringBuilder("from User u where 1=1");
        Map<String, Object> params = new HashMap<>();

        if (!StringUtils.isBlank(search)) {
            query.append(" and (lower(u.username) like :search or lower(u.email) like :search)");
            params.put("search", "%" + search.toLowerCase().trim() + "%");
        }

        if (!StringUtils.isBlank(username)) {
            query.append(" and lower(u.username) like :username");
            params.put("username", "%" + username.toLowerCase().trim() + "%");
        }

        if (!StringUtils.isBlank(email)) {
            query.append(" and lower(u.email) like :email");
            params.put("email", "%" + email.toLowerCase().trim() + "%");
        }

        if (role != null) {
            query.append(" and u.role = :role");
            params.put("role", role);
        }

        if (enabled != null) {
            query.append(" and u.enabled = :enabled");
            params.put("enabled", enabled);
        }

        return find(query.toString(), sort, params).page(Page.of(page, size));
    }

    @Transactional
    public void setEnabled(User user, Boolean enabled) {
        User managedUser = em.merge(user);
        managedUser.setEnabled(enabled);
    }
}
