package org.sagebionetworks.repo.manager.util;

import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.util.ValidateArgument;

public class UserAccessRestrictionUtils {

    public static boolean doesUserHaveUnmetAccessRestrictionsForEntity(UserEntityPermissionsState userEntityPermissionsState,
                                                                       UsersRestrictionStatus usersRestrictionStatus) {
        ValidateArgument.required(userEntityPermissionsState, "userEntityPermissionsState");
        ValidateArgument.required(usersRestrictionStatus, "usersRestrictionStatus");

        boolean isUserDataContributor = userEntityPermissionsState.hasUpdate() && userEntityPermissionsState.hasDelete();
        for (UsersRequirementStatus requirementStatus : usersRestrictionStatus.getAccessRestrictions()) {
            boolean isExemptionEligible = requirementStatus.isExemptionEligible();
            boolean isUserExempted = isUserDataContributor && isExemptionEligible;
            if (!isUserExempted && requirementStatus.isUnmet()) {
                return true;
            }
        }
        return false;
    }

    public static boolean doesUserHaveUnmetAccessRestrictionsForNonEntity(UsersRestrictionStatus usersRestrictionStatus) {
        ValidateArgument.required(usersRestrictionStatus, "usersRestrictionStatus");
        for (UsersRequirementStatus requirementStatus : usersRestrictionStatus.getAccessRestrictions()) {
            if (requirementStatus.isUnmet()) {
                return true;
            }
        }
        return false;
    }
}
