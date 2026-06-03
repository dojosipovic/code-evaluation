package com.codeevaluation.core.helper;

import com.codeevaluation.core.model.Task;
import com.codeevaluation.core.model.User;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskAccessPolicy {

    public boolean canModifyTask(Task task, User currentUser) {
        return currentUser.isAdmin() || task.isOwner(currentUser.getUsername());
    }

    public boolean canUseTask(Task task, User currentUser) {
        boolean published = task.isPublished();
        boolean admin = currentUser.isAdmin();
        boolean enabled = Boolean.TRUE.equals(task.getEnabled());

        return published && (admin || enabled);
    }
}
