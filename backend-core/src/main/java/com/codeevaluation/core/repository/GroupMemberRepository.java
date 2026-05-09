package com.codeevaluation.core.repository;

import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.GroupMember;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class GroupMemberRepository implements PanacheRepository<GroupMember> {

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        delete("group.id = ?1 and user.id = ?2", groupId, userId);
    }

    public GroupMember addMember(Group group, User user) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);

        group.getMembers().add(groupMember);
        persist(groupMember);

        return groupMember;
    }

    public PanacheQuery<GroupMember> getMembers(Long groupId, PagedContext pagedContext) {
        StringBuilder query = new StringBuilder(
                """
                    from GroupMember gm
                    join fetch gm.user
                    where gm.group.id = :groupId
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);

        if (!StringUtils.isBlank(pagedContext.search())) {
            query.append(
                    """
                     and (
                              lower(gm.user.username) like :search
                              or lower(gm.user.firstname) like :search
                              or lower(gm.user.lastname) like :search
                              or lower(concat(gm.user.firstname, ' ', gm.user.lastname)) like :search
                              or lower(gm.user.email) like :search
                        )
                    """);

            params.put("search", "%" + pagedContext.search().toLowerCase().trim() + "%");
        }

        Sort sort = pagedContext.sort();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return find(query.toString(), sort, params).page(Page.of(page, size));
    }
}
