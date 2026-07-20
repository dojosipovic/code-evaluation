package com.codeevaluation.core.helper;

import com.codeevaluation.core.model.User;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlagScanClusterAccessPolicy {

    public boolean canSeeAllClusters(User currentUser) {
        return currentUser.isAdmin();
    }
}
