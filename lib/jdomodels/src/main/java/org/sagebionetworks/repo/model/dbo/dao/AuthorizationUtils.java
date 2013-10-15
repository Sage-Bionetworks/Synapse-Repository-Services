package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;

public class AuthorizationUtils {
	
	public static boolean isUserAnonymous(UserInfo userInfo) {
		return isUserAnonymous(userInfo.getIndividualGroup());
	}
	
	public static boolean isUserAnonymous(UserGroup ug) {
		return isUserAnonymous(ug.getName());
	}
	
	public static boolean isUserAnonymous(String username) {
		return AuthorizationConstants.ANONYMOUS_USER_ID.equals(username);
	}
}
