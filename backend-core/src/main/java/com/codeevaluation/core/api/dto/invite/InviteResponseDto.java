package com.codeevaluation.core.api.dto.invite;

import com.codeevaluation.core.enumeration.InviteStatus;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.Invite;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteResponseDto {
    private Long id;
    private String email;
    private Role role;
    private InviteStatus status;
    private Instant expiresAt;
    private Instant createdAt;

    public static InviteResponseDto from(Invite invite) {
        InviteResponseDto dto = new InviteResponseDto();
        dto.setId(invite.getId());
        dto.setEmail(invite.getEmail());
        dto.setRole(invite.getRole());
        dto.setStatus(invite.getStatus());
        dto.setExpiresAt(invite.getExpiresAt());
        dto.setCreatedAt(invite.getCreatedAt());
        return dto;
    }
}
