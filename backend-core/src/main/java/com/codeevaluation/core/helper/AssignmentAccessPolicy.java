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
}
