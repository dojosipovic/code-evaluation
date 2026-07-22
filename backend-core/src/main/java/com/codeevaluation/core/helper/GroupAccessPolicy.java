package com.codeevaluation.core.helper;

import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.User;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GroupAccessPolicy {

    public boolean canFetchGroup(Group group, User currentUser) {
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername())
                || group.isMember(currentUser.getUsername());
    }

    public boolean canModifyGroup(Group group, User currentUser) {
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername());
    }

    public boolean canCreateAssignment(Group group, User currentUser) {
        return canModifyGroup(group, currentUser);
    }

    public boolean canFetchAssignments(Group group, User currentUser) {
        return canFetchGroup(group, currentUser);
    }

    public boolean canSeeAssignmentsTask(Group group, User currentUser) {
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername());
    }

    public boolean canSeeAssignmentRequiresValuation(Group group, User currentUser) {
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername());
    }
}
