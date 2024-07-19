package org.sagebionetworks.repo.manager.util;

import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.util.ValidateArgument;

import java.util.ArrayList;
import java.util.List;

public class UserAccessRestrictionUtils {
	
	/**
	 * @param userEntityPermissionsState
	 * @return True if the user has update and delete permissions according to the given {@link UserEntityPermissionsState}
	 */
	public static boolean isUserDataContributor(UserEntityPermissionsState userEntityPermissionsState) {
		return userEntityPermissionsState.hasUpdate() && userEntityPermissionsState.hasDelete();
	}
	
	/**
	 * @param userRequirementStatus
	 * @param isUserDataContributor
	 * @return True if the user is the given {@link UsersRequirementStatus#isExemptionEligible()} is true and the given isUserDataContributor is true
	 */
	public static boolean isUserExempt(UsersRequirementStatus userRequirementStatus, boolean isUserDataContributor) {
		return isUserDataContributor && userRequirementStatus.isExemptionEligible();
	}

    public static List<Long> getUsersUnmetAccessRestrictionsForEntity(UserEntityPermissionsState userEntityPermissionsState, UsersRestrictionStatus usersRestrictionStatus) {
    	ValidateArgument.required(userEntityPermissionsState, "userEntityPermissionsState");
        ValidateArgument.required(usersRestrictionStatus, "usersRestrictionStatus");
        List<Long> unmetAccessRequirements = new ArrayList<>();
        boolean isUserDataContributor = isUserDataContributor(userEntityPermissionsState);
        for (UsersRequirementStatus requirementStatus : usersRestrictionStatus.getAccessRestrictions()) {
            boolean isUserExempted = isUserExempt(requirementStatus, isUserDataContributor);
            if (!isUserExempted && requirementStatus.isUnmet()) {
                unmetAccessRequirements.add(requirementStatus.getRequirementId());
            }
        }
        return unmetAccessRequirements;
    }

    public static List<Long> getUsersUnmetAccessRestrictionsForNonEntity(UsersRestrictionStatus usersRestrictionStatus) {
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
