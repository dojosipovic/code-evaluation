package com.codeevaluation.core.helper;

import com.codeevaluation.core.api.dto.group.GroupCreateDto;
import com.codeevaluation.core.api.dto.group.GroupUpdateDto;
import com.codeevaluation.core.model.Group;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class GroupValidator {

    public void validateGroup(GroupCreateDto groupCreateDto) {
        if (groupCreateDto == null) {
            throw new BadRequestException("Payload is required");
        }

        validateName(groupCreateDto.name());
    }

    public void validateGroup(GroupUpdateDto groupUpdateDto) {
        if (groupUpdateDto == null) {
            throw new BadRequestException("Payload is required");
        }

        validateName(groupUpdateDto.getName());
    }

    private void validateName(String name) {
        name = StringUtils.trimToEmpty(name);

        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("Name is required");
        }

        if (name.length() > Group.NAME_MAX_LENGTH) {
            throw new BadRequestException(
                    "Name can have at most " + Group.NAME_MAX_LENGTH + " characters.");
        }
    }
}
