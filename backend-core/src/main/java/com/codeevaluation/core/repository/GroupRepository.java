package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.PagedResponse;
import com.codeevaluation.core.api.dto.group.GroupListItemDto;
import com.codeevaluation.core.api.dto.group.GroupUpdateDto;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.service.dto.GroupListItemProjection;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

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

    public PagedResponse<GroupListItemDto> getGroups(
            User currentUser, PagedContext pagedContext
    ) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder(" where 1 = 1 ");

        if (!currentUser.isAdmin()) {
            where.append(
                    """
                     and (
                        g.owner.id = :currentUserId
                        or exists (
                            select 1
                            from GroupMember gm2
                            where gm2.group = g
                            and gm2.user.id = :currentUserId
                        )
                    )
                    """
            );
            params.put("currentUserId", currentUser.getId());
        }
        if (!StringUtils.isBlank(pagedContext.search())) {
            where.append(
                    """
                         and (
                            lower(g.name) like :search
                            or lower(g.description) like :search
                            or lower(g.owner.firstname) like :search
                            or lower(g.owner.lastname) like :search
                            or lower(concat(g.owner.firstname, ' ', g.owner.lastname)) like :search
                        )
                    """
            );
            params.put("search", "%" + pagedContext.search().toLowerCase().trim() + "%");
        }

        String dataJpql =
                """
                select new com.codeevaluation.core.service.dto.GroupListItemProjection(
                    g.id,
                    g.name,
                    g.description,
                    g.createdAt,
                    count(distinct gm.id),
                    g.owner
                )
                from Group g
                left join g.members gm
                """
                + where
                +
                """
                group by g.id, g.name, g.description, g.createdAt, g.owner
                """ + toOrderBy(pagedContext.sort());

        var dataQuery = getEntityManager()
                .createQuery(dataJpql, GroupListItemProjection.class);

        params.forEach(dataQuery::setParameter);

        dataQuery.setFirstResult(pagedContext.page() * pagedContext.size());
        dataQuery.setMaxResults(pagedContext.size());

        List<GroupListItemProjection> items = dataQuery.getResultList();

        String countJpql =
                """
                select count(distinct g.id)
                from Group g
                """ + where;

        var countQuery = getEntityManager()
                .createQuery(countJpql, Long.class);

        params.forEach(countQuery::setParameter);

        long totalItems = countQuery.getSingleResult();

        return new PagedResponse<>(
                GroupListItemDto.from(items),
                pagedContext.page(),
                pagedContext.size(),
                totalItems
        );
    }

    private String toOrderBy(Sort sort) {
        if (sort == null || sort.getColumns().isEmpty()) {
            return " order by g.name asc";
        }

        return " order by " + sort.getColumns().stream()
                .map(c -> c.getName() + " "
                        + (c.getDirection() == Sort.Direction.Ascending ? "asc" : "desc"))
                .collect(Collectors.joining(", "));
    }
}
