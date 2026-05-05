package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.group.GroupCreateDto;
import com.codeevaluation.core.api.dto.group.GroupResponseDto;
import com.codeevaluation.core.helper.GroupValidator;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.GroupRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
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
}
