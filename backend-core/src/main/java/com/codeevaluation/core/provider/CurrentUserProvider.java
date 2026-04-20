package com.codeevaluation.core.provider;

import com.codeevaluation.core.model.User;
import com.codeevaluation.core.repository.UserRepository;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final SecurityIdentity securityIdentity;
    private final UserRepository userRepository;

    public User getCurrentUser() {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            throw new NotAuthorizedException("User is not authenticated.");
        }

        String principalName = securityIdentity.getPrincipal().getName();

        return userRepository.findByUsername(principalName)
                .orElseThrow(() -> new NotAuthorizedException("Authenticated user not found"));
    }
}
