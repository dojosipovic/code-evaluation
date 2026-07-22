package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.assignment.AssignmentCreateDto;
import com.codeevaluation.core.api.dto.assignment.AssignmentFilterParams;
import com.codeevaluation.core.enumeration.Role;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    public PanacheQuery<Assignment> findAssignments(
            PagedContext pagedContext,
            AssignmentFilterParams filterParams
    ) {
        StringBuilder query = new StringBuilder(
                """
                    select distinct a from Assignment a
                    join fetch a.createdBy createdBy
                    join fetch a.task task
                    join fetch task.user taskUser
                    join fetch a.group g
                    left join Submission filteredSubmission
                        on filteredSubmission.assignment = a
                        and filteredSubmission.user.id = :filterUserId
                    where 1=1
                """);

        Map<String, Object> params = new HashMap<>();
        params.put("filterUserId", filterParams.currentUserId());

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

        appendAccessFilter(query, params, filterParams);
        appendAssignmentFilters(query, params, filterParams);
        appendSubmissionFilters(query, params, filterParams);

        Sort sort = pagedContext.sort();
        int page = pagedContext.page();
        int size = pagedContext.size();

        return find(query.toString(), sort, params).page(Page.of(page, size));
    }

    private void appendAccessFilter(
            StringBuilder query,
            Map<String, Object> params,
            AssignmentFilterParams filterParams
    ) {
        User currentUser = filterParams.user();
        if (currentUser == null) {
            return;
        }

        if (currentUser.isAdmin()) {
            return;
        }

        query.append(
                """
                and (
                    g.owner.id = :currentUserId
                    or exists (
                        select 1 from GroupMember gm
                        where gm.group = g
                        and gm.user.id = :currentUserId
                    )
                )
                """);

        params.put("currentUserId", currentUser.getId());
    }

    private void appendAssignmentFilters(
            StringBuilder query,
            Map<String, Object> params,
            AssignmentFilterParams filterParams
    ) {
        if (filterParams.groupId() != null) {
            query.append(" and g.id = :groupId");
            params.put("groupId", filterParams.groupId());
        }

        if (filterParams.active() != null) {
            Instant now = Instant.now();
            if (filterParams.active()) {
                query.append(" and a.startsAt < :now and a.endsAt > :now");
            } else {
                query.append(" and (a.startsAt >= :now or a.endsAt <= :now)");
            }
            params.put("now", now);
        }
    }

    private void appendSubmissionFilters(
            StringBuilder query,
            Map<String, Object> params,
            AssignmentFilterParams filterParams
    ) {
        if (filterParams.submitted() != null) {
            if (filterParams.submitted()) {
                query.append(" and filteredSubmission.id is not null");
            } else {
                query.append(" and filteredSubmission.id is null");
            }
        }

        if (filterParams.ungraded() != null) {
            if (filterParams.ungraded()) {
                appendUngradedFilter(query, params, filterParams.user());
            } else {
                query.append(" and filteredSubmission.finalScore is not null");
            }
        }
    }

    private void appendUngradedFilter(
            StringBuilder query,
            Map<String, Object> params,
            User currentUser
    ) {
        if (currentUser != null && currentUser.isAdmin()) {
            appendAnyUngradedSubmissionExists(query);
            return;
        }

        if (currentUser != null && currentUser.getRole() == Role.PROF) {
            query.append(" and g.owner.id = :ungradedOwnerId");
            params.put("ungradedOwnerId", currentUser.getId());
            appendAnyUngradedSubmissionExists(query);
            return;
        }

        query.append(
                " and filteredSubmission.id is not null"
                        + " and filteredSubmission.finalScore is null"
                        + " and a.endsAt <= :ungradedNow"
        );
        params.put("ungradedNow", Instant.now());
    }

    private void appendAnyUngradedSubmissionExists(StringBuilder query) {
        query.append(
                """
                and exists (
                    select 1
                    from Submission ungradedSubmission
                    where ungradedSubmission.assignment = a
                    and ungradedSubmission.finalScore is null
                )
                """
        );
    }

    public Optional<Assignment> findByIdWithTaskAndTests(Long assignmentId) {
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
        ).stream().findFirst();
    }

    public Optional<Assignment> findByIdWithReminderRelations(Long assignmentId) {
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
        ).stream().findFirst();
    }
}
