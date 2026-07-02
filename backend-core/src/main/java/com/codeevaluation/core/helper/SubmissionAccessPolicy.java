package com.codeevaluation.core.helper;

import com.codeevaluation.core.model.Submission;
import com.codeevaluation.core.model.User;

public class SubmissionAccessPolicy {

    private SubmissionAccessPolicy() {}

    public static boolean canSeeSubmission(Submission submission, User currentUser) {
        return currentUser.isAdmin()
                || submission.getAssignment().getGroup().isOwner(currentUser.getUsername())
                || submission.getUser().getId().equals(currentUser.getId());
    }
}
