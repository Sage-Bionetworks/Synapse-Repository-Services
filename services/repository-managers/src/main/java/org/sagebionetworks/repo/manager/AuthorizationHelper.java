package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;

public class AuthorizationHelper {
	public static boolean isUserAnonymous(UserInfo userInfo) {
		return AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId());
	}
}
