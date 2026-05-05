package com.codeevaluation.core.api.dto.group;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.model.Group;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupResponseDto {
    private Long id;
    private String name;
    private String description;
    private UserDto owner;
    private Instant createdAt;

    public static GroupResponseDto from(Group group) {
        GroupResponseDto groupResponseDto = new GroupResponseDto();
        groupResponseDto.setId(group.getId());
        groupResponseDto.setName(group.getName());
        groupResponseDto.setDescription(group.getDescription());
        groupResponseDto.setOwner(UserDto.from(group.getOwner()));
        groupResponseDto.setCreatedAt(group.getCreatedAt());

        return groupResponseDto;
    }
}
