package com.codeevaluation.core.service;

import com.codeevaluation.core.model.User;
import com.codeevaluation.core.repository.UserRepository;
import com.codeevaluation.core.util.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotAuthorizedException("Invalid credentials", "Bearer"));

        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new NotAuthorizedException("Invalid credentials", "Bearer");
        }

        return user;
    }

}
