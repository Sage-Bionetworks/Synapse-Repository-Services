package org.sagebionetworks.repo.manager.util;

import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.util.ValidateArgument;

import java.util.ArrayList;
import java.util.List;

public class UserAccessRestrictionUtils {

    public static List<Long> usersUnmetAccessRestrictionsForEntity(UserEntityPermissionsState userEntityPermissionsState,
                                                                   UsersRestrictionStatus usersRestrictionStatus) {
        ValidateArgument.required(userEntityPermissionsState, "userEntityPermissionsState");
        ValidateArgument.required(usersRestrictionStatus, "usersRestrictionStatus");
        List<Long> unmetAccessRequirements = new ArrayList<>();
        boolean isUserDataContributor = userEntityPermissionsState.hasUpdate() && userEntityPermissionsState.hasDelete();
        for (UsersRequirementStatus requirementStatus : usersRestrictionStatus.getAccessRestrictions()) {
            boolean isExemptionEligible = requirementStatus.isExemptionEligible();
            boolean isUserExempted = isUserDataContributor && isExemptionEligible;
            if (!isUserExempted && requirementStatus.isUnmet()) {
                unmetAccessRequirements.add(requirementStatus.getRequirementId());
            }
        }
        return unmetAccessRequirements;
    }

    public static List<Long> usersUnmetAccessRestrictionsForNonEntity(UsersRestrictionStatus usersRestrictionStatus) {
        ValidateArgument.required(usersRestrictionStatus, "usersRestrictionStatus");
        List<Long> unmetAccessRequirements = new ArrayList<>();
        for (UsersRequirementStatus requirementStatus : usersRestrictionStatus.getAccessRestrictions()) {
            if (requirementStatus.isUnmet()) {
                unmetAccessRequirements.add(requirementStatus.getRequirementId());
            }
        }
        return unmetAccessRequirements;
    }
}
