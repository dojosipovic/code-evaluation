package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.group.GroupCreateDto;
import com.codeevaluation.core.api.dto.group.GroupResponseDto;
import com.codeevaluation.core.api.dto.group.GroupUpdateDto;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.helper.GroupValidator;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.helper.PagedParams;
import com.codeevaluation.core.helper.PagedSearchGroupMemberImpl;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.GroupMember;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.GroupMemberRepository;
import com.codeevaluation.core.repository.GroupRepository;
import com.codeevaluation.core.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
@RequiredArgsConstructor
public class GroupService {

    private final CurrentUserProvider currentUserProvider;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupValidator groupValidator;
    private final PagedSearchGroupMemberImpl pagedSearchGroupMember;

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

    @Transactional
    public void addMember(Long groupId, Long userId) {
        Group group = findGroupForModification(groupId);
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (group.isMember(user.getUsername())) {
            throw new WebApplicationException("User is already a member of this group",
                    Response.Status.CONFLICT);
        }

        groupMemberRepository.addMember(group, user);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        Group group = findGroupForModification(groupId);
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!group.isMember(user.getUsername())) {
            throw new NotFoundException("Group member not found");
        }

        groupMemberRepository.removeMember(group.getId(), user.getId());
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

    private Group findGroupForModification(Long groupId) {
        Group group = groupRepository.findByIdOptional(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!canModifyGroup(group)) {
            throw new ForbiddenException("You cannot update this group");
        }

        return group;
    }

    public PagedResponse<UserDto> getMembers(Long groupId, PagedParams pagedParams) {
        Group group = groupRepository.findByIdOptional(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!canFetchGroup(group)) {
            throw new ForbiddenException("You cannot see this group");
        }

        PagedContext pagedContext = pagedSearchGroupMember.generateFrom(pagedParams);
        PanacheQuery<GroupMember> query = groupMemberRepository.getMembers(groupId, pagedContext);

        List<UserDto> items = query.list()
                .stream()
                .map(GroupMember::getUser)
                .map(UserDto::from)
                .toList();
        long totalItems = query.count();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return new PagedResponse<>(items, page, size, totalItems);
    }
}
