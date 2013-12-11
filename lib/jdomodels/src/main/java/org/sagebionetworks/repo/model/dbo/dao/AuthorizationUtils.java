package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;

public class AuthorizationUtils {
	
	public static boolean isUserAnonymous(UserInfo userInfo) {
		return isUserAnonymous(userInfo.getIndividualGroup());
	}
	
	public static boolean isUserAnonymous(UserGroup ug) {
		return isUserAnonymous(ug.getId());
	}
	
	public static boolean isUserAnonymous(String id) {
		return BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString().equals(id);
	}
}
