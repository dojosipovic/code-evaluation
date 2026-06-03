package com.codeevaluation.core.repository;

import com.codeevaluation.core.api.dto.assignment.AssignmentCreateDto;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
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
}
