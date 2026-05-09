package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.group.GroupCreateDto;
import com.codeevaluation.core.api.dto.group.GroupResponseDto;
import com.codeevaluation.core.api.dto.group.GroupUpdateDto;
import com.codeevaluation.core.helper.GroupValidator;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.GroupRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@RequiredArgsConstructor
public class GroupService {

    private final CurrentUserProvider currentUserProvider;
    private final GroupRepository groupRepository;
    private final GroupValidator groupValidator;

    @Transactional
    public GroupResponseDto createGroup(GroupCreateDto groupCreateDto) {
        groupValidator.validateGroup(groupCreateDto);
        User currentUser = currentUserProvider.getCurrentUser();
        Group group = groupRepository.create(
                groupCreateDto.name(),
                StringUtils.trimToNull(groupCreateDto.description()),
                currentUser);

        return GroupResponseDto.from(group);
    }

    @Transactional
    public GroupResponseDto findById(Long id) {
        Group group = groupRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!canFetchGroup(group)) {
            throw new ForbiddenException("You cannot see this group");
        }

        return GroupResponseDto.from(group);
    }

    @Transactional
    public GroupResponseDto updateGroup(GroupUpdateDto groupUpdateDto, Long id) {
        groupValidator.validateGroup(groupUpdateDto);
        Group group = groupRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!canModifyGroup(group)) {
            throw new ForbiddenException("You cannot update this group");
        }

        groupUpdateDto.setDescription(StringUtils.trimToNull(groupUpdateDto.getDescription()));
        return GroupResponseDto.from(groupRepository.update(group, groupUpdateDto));
    }

    private boolean canFetchGroup(Group group) {
        User currentUser = currentUserProvider.getCurrentUser();
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername())
                || group.isMember(currentUser.getUsername());
    }

    private boolean canModifyGroup(Group group) {
        User currentUser = currentUserProvider.getCurrentUser();
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername());
    }
}
