package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.assignment.AssignmentCreateDto;
import com.codeevaluation.core.helper.PagedContext;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.Task;
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
public class AssignmentRepository implements PanacheRepository<Assignment> {

    @Transactional
    public Assignment create(
            AssignmentCreateDto assignmentCreateDto,
            Group group,
            Task task,
            User createdBy
    ) {
        Assignment assignment = new Assignment();

        assignment.setName(StringUtils.trimToEmpty(assignmentCreateDto.name()));
        assignment.setStartsAt(assignmentCreateDto.startsAt());
        assignment.setEndsAt(assignmentCreateDto.endsAt());
        assignment.setPoints(assignmentCreateDto.points());
        assignment.setGroup(group);
        assignment.setTask(task);
        assignment.setCreatedBy(createdBy);

        persist(assignment);

        return assignment;
    }

    public PanacheQuery<Assignment> getGroupAssignments(Long groupId, PagedContext pagedContext) {
        StringBuilder query = new StringBuilder(
                """
                    select distinct a from Assignment a
                    join fetch a.createdBy createdBy
                    join fetch a.task task
                    join fetch task.user taskUser
                    where a.group.id = :groupId
                """);

        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);

        if (!StringUtils.isBlank(pagedContext.search())) {
            query.append(
                    """
                     and (
                            lower(a.name) like :search
                            or lower(task.title) like :search
                            or lower(createdBy.username) like :search
                            or lower(createdBy.firstname) like :search
                            or lower(createdBy.lastname) like :search
                            or lower(concat(createdBy.firstname, ' ', createdBy.lastname)) like :search
                        )
                    """);

            params.put("search", "%" + pagedContext.search().toLowerCase().trim() + "%");
        }

        Sort sort = pagedContext.sort();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return find(query.toString(), sort, params).page(Page.of(page, size));
    }

    public java.util.Optional<Assignment> findByIdWithTaskAndTests(Long assignmentId) {
        return find(
                """
                    select distinct a from Assignment a
                    join fetch a.group g
                    join fetch a.task t
                    left join fetch t.tests tests
                    left join fetch t.user taskUser
                    join fetch a.createdBy createdBy
                    where a.id = :assignmentId
                """,
                Map.of("assignmentId", assignmentId)
        ).firstResultOptional();
    }

    public java.util.Optional<Assignment> findByIdWithReminderRelations(Long assignmentId) {
        return find(
                """
                    select distinct a from Assignment a
                    join fetch a.group grp
                    left join fetch grp.members members
                    left join fetch members.user memberUser
                    join fetch a.task task
                    join fetch a.createdBy createdBy
                    where a.id = :assignmentId
                """,
                Map.of("assignmentId", assignmentId)
        ).firstResultOptional();
    }
}
