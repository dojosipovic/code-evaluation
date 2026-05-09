package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.group.GroupUpdateDto;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GroupRepository implements PanacheRepository<Group> {

    public Group create(String name, String description, User owner) {
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setOwner(owner);

        persist(group);

        return group;
    }

    public Group update(Group group, GroupUpdateDto groupUpdateDto) {
        group.setName(groupUpdateDto.getName());
        group.setDescription(groupUpdateDto.getDescription());

        persist(group);

        return group;
    }
}
