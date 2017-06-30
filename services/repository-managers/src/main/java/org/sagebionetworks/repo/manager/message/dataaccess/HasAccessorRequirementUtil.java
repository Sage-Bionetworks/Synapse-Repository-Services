package org.sagebionetworks.repo.manager.message.dataaccess;

import java.util.Set;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.util.ValidateArgument;

public class HasAccessorRequirementUtil {

	public static void validateHasAccessorRequirement(HasAccessorRequirement req,
			Set<String> accessors, GroupMembersDAO groupMembersDao, VerificationDAO verificationDao){
		if (req.getIsCertifiedUserRequired()) {
			ValidateArgument.requirement(groupMembersDao.areMemberOf(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
					accessors),
					"Accessors must be Synapse Certified Users.");
		}
		if (req.getIsValidatedProfileRequired()) {
			ValidateArgument.requirement(verificationDao.haveValidatedProfiles(accessors),
					"Accessors must have validated profiles.");
		}
	}
}
