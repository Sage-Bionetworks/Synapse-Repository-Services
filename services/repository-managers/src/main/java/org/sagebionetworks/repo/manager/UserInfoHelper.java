package org.sagebionetworks.repo.manager;

import java.util.HashSet;
import java.util.Set;

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
	
	public static UserInfo createAnonymousUserInfo() {
		UserInfo result = new UserInfo(false);
		result.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
		Set<Long> groups = new HashSet<Long>();
		// Everyone belongs to their own group and to Public
		groups.add(result.getId());
		groups.add(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		result.setGroups(groups);
		return result;
	}
}
