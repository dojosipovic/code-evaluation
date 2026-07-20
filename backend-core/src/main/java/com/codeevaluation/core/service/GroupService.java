package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.group.GroupCreateDto;
import com.codeevaluation.core.api.dto.group.GroupLeaderboardDto;
import com.codeevaluation.core.api.dto.group.GroupListItemDto;
import com.codeevaluation.core.api.dto.group.GroupMemberDto;
import com.codeevaluation.core.api.dto.group.GroupResponseDto;
import com.codeevaluation.core.api.dto.group.GroupUpdateDto;
import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.helper.GroupAccessPolicy;
import com.codeevaluation.core.helper.GroupValidator;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.api.query.PagedParams;
import com.codeevaluation.core.helper.PagedSearchGroupImpl;
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
    private final PagedSearchGroupImpl pagedSearchGroup;
    private final GroupAccessPolicy groupAccessPolicy;

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

        if (!groupAccessPolicy.canFetchGroup(group, currentUserProvider.getCurrentUser())) {
            throw new ForbiddenException("You cannot see this group");
        }

        return GroupResponseDto.from(group);
    }

    @Transactional
    public GroupResponseDto updateGroup(GroupUpdateDto groupUpdateDto, Long id) {
        groupValidator.validateGroup(groupUpdateDto);
        Group group = groupRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!groupAccessPolicy.canModifyGroup(group, currentUserProvider.getCurrentUser())) {
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

    private Group findGroupForModification(Long groupId) {
        Group group = groupRepository.findByIdOptional(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!groupAccessPolicy.canModifyGroup(group, currentUserProvider.getCurrentUser())) {
            throw new ForbiddenException("You cannot update this group");
        }

        return group;
    }

    public PagedResponse<GroupMemberDto> getMembers(Long groupId, PagedParams pagedParams) {
        Group group = groupRepository.findByIdOptional(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!groupAccessPolicy.canFetchGroup(group, currentUserProvider.getCurrentUser())) {
            throw new ForbiddenException("You cannot see this group");
        }

        PagedContext pagedContext = pagedSearchGroupMember.generateFrom(pagedParams);
        PanacheQuery<GroupMember> query = groupMemberRepository.getMembers(groupId, pagedContext);

        List<GroupMemberDto> items = GroupMemberDto.from(query.list());
        long totalItems = query.count();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return new PagedResponse<>(items, page, size, totalItems);
    }

    public PagedResponse<UserDto> getNonMembers(Long groupId, PagedParams pagedParams) {
        Group group = groupRepository.findByIdOptional(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!groupAccessPolicy.canFetchGroup(group, currentUserProvider.getCurrentUser())) {
            throw new ForbiddenException("You cannot see this group");
        }

        PagedContext pagedContext = pagedSearchGroupMember.generateFrom(pagedParams);
        PanacheQuery<User> query = groupMemberRepository.getNonMembers(groupId, pagedContext);

        List<UserDto> items = UserDto.from(query.list());
        long totalItems = query.count();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return new PagedResponse<>(items, page, size, totalItems);
    }

    public List<GroupLeaderboardDto> getLeaderboard(Long groupId) {
        Group group = groupRepository.findByIdOptional(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (!groupAccessPolicy.canFetchGroup(group, currentUserProvider.getCurrentUser())) {
            throw new ForbiddenException("You cannot see this group leaderboard");
        }

        return groupMemberRepository.getLeaderboard(groupId);
    }

    public PagedResponse<GroupListItemDto> getGroups(PagedParams pagedParams) {
        User currentUser = currentUserProvider.getCurrentUser();
        PagedContext pagedContext = pagedSearchGroup.generateFrom(pagedParams);
        return groupRepository.getGroups(currentUser, pagedContext);
    }
}
