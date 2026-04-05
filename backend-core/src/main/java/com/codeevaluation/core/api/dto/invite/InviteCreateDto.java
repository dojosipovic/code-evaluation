package com.codeevaluation.core.api.dto.invite;

import com.codeevaluation.core.enumeration.Role;

public record InviteCreateDto(String email, Role role) {
}
