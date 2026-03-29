package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto findByEmail(String email) {

        if (StringUtils.isBlank(email)) {
            throw new BadRequestException("Email is mandatory");
        }

        return userRepository.findByEmail(email)
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public UserDto findByUsername(String username) {

        if (StringUtils.isBlank(username)) {
            throw new BadRequestException("Username is mandatory");
        }

        return userRepository.findByUsername(username)
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
