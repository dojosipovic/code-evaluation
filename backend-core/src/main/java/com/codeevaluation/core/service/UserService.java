package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
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

    public PagedResponse<UserDto> getUsers(
            int page,
            int size,
            String username,
            String email,
            String search,
            Role role,
            Boolean enabled,
            String sortBy,
            String sortDirection
    ) {
        validatePageParams(page, size);

        Sort sort = buildSort(sortBy, sortDirection);

        PanacheQuery<User> query = userRepository.search(
                username,
                email,
                search,
                role,
                enabled,
                sort,
                page,
                size
        );

        List<UserDto> items = query.list()
                .stream()
                .map(UserDto::from)
                .toList();

        long totalItems = query.count();

        return new PagedResponse<>(items, page, size, totalItems);
    }

    private void validatePageParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        String safeSortBy = (StringUtils.isBlank(sortBy)) ? "id" : sortBy;
        String safeSortDirection = (StringUtils.isBlank(sortDirection)) ? "desc" : sortDirection;

        String mappedField = switch (safeSortBy) {
            case "id" -> "id";
            case "username" -> "username";
            case "email" -> "email";
            case "role" -> "role";
            case "enabled" -> "enabled";
            default -> throw new IllegalArgumentException("Unsupported sortBy: " + safeSortBy);
        };

        return switch (safeSortDirection.toLowerCase()) {
            case "asc" -> Sort.ascending(mappedField);
            case "desc" -> Sort.descending(mappedField);
            default -> throw new IllegalArgumentException(
                    "Unsupported sortDirection: " + safeSortDirection);
        };
    }

    public void setEnabled(Long id, Boolean enabled, String username) {
        if (enabled == null) {
            throw new BadRequestException("enabled property required");
        }

        User user = userRepository.findByIdOptional(id)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getUsername().equals(username)) {
            throw new BadRequestException("You cannot enable or disable yourself");
        }

        userRepository.setEnabled(user, enabled);
    }
}
