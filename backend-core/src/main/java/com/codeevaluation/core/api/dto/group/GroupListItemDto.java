package com.codeevaluation.core.api.dto.group;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.service.dto.GroupListItemProjection;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupListItemDto {
    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private Long memberCount;
    private UserDto owner;

    public static GroupListItemDto from(GroupListItemProjection groupListItemProjection) {
        GroupListItemDto groupListItemDto = new GroupListItemDto();
        groupListItemDto.setId(groupListItemProjection.id());
        groupListItemDto.setName(groupListItemProjection.name());
        groupListItemDto.setDescription(groupListItemProjection.description());
        groupListItemDto.setOwner(UserDto.from(groupListItemProjection.owner()));
        groupListItemDto.setCreatedAt(groupListItemProjection.createdAt());
        groupListItemDto.setMemberCount(groupListItemProjection.memberCount());

        return groupListItemDto;
    }

    public static List<GroupListItemDto> from(
            List<GroupListItemProjection> groupListItemProjections
    ) {
        return groupListItemProjections.stream().map(GroupListItemDto::from).toList();
    }
}
