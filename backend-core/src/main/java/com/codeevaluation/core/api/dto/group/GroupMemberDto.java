package com.codeevaluation.core.api.dto.group;

import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.GroupMember;
import com.codeevaluation.core.model.User;
import java.time.Instant;
import java.util.List;

public record GroupMemberDto(
        Long id,
        String username,
        String firstname,
        String lastname,
        String email,
        Role role,
        Boolean enabled,
        Instant addedAt
) {
    public static GroupMemberDto from(GroupMember gm) {
        User user = gm.getUser();

        return new GroupMemberDto(
                user.getId(),
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getRole(),
                user.getEnabled(),
                gm.getAddedAt()
        );
    }

    public static List<GroupMemberDto> from(List<GroupMember> groupMembers) {
        return groupMembers.stream().map(GroupMemberDto::from).toList();
    }
}
