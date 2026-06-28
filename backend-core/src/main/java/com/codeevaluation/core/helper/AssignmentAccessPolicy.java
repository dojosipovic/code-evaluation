package com.codeevaluation.core.helper;

import com.codeevaluation.core.model.Group;
import com.codeevaluation.core.model.User;
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

    public boolean canSubmitAssignment(Group group, User currentUser) {
        return currentUser.isAdmin()
                || group.isOwner(currentUser.getUsername())
                || group.isMember(currentUser.getUsername());
    }
}
