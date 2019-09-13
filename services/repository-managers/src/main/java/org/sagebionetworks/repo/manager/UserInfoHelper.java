package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;

public class UserInfoHelper {
	public static boolean isCertified(UserInfo userInfo) {
		return userInfo.getGroups().contains(
				BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
	}

	public static boolean isACTMember(UserInfo userInfo) {
		return userInfo.getGroups().contains(TeamConstants.ACT_TEAM_ID);
	}
}
