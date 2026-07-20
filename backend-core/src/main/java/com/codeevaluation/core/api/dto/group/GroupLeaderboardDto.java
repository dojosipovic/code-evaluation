package com.codeevaluation.core.api.dto.group;

import com.codeevaluation.core.api.dto.user.UserDto;
import java.math.BigDecimal;

public record GroupLeaderboardDto(
        UserDto user,
        BigDecimal totalScore
) {}
