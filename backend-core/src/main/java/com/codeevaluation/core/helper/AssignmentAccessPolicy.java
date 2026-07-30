package com.codeevaluation.core.helper;

import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.User;
import java.time.Instant;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AssignmentAccessPolicy {

    public boolean showTestExpectedOutput(Group group, User currentUser) {
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername());
    }

    public boolean canSeeAssignment(Group group, User currentUser) {
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername())
                || group.isMember(currentUser.getUsername());
    }

    public boolean canSeeAssignmentDetails(Assignment assignment, User currentUser) {
        return currentUser.getRole() != Role.STUDENT
                || !assignment.getStartsAt().isAfter(Instant.now());
    }

    public boolean canSubmitAssignment(Group group, User currentUser) {
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername())
                || group.isMember(currentUser.getUsername());
    }

    public boolean canEvaluateAssignment(Assignment assignment, User currentUser) {
        return currentUser.isAdmin()
                || assignment.getGroup().isOwner(currentUser.getUsername())
                || assignment.isOwner(currentUser.getUsername());
    }

    public boolean canIssuePlagScanToken(Assignment assignment, User currentUser) {
        return currentUser.isAdmin()
                || (currentUser.getRole() == Role.PROF
                && (assignment.getGroup().isOwner(currentUser.getUsername())
                || assignment.isOwner(currentUser.getUsername())));
    }
}
