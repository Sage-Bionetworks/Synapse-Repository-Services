package org.sagebionetworks.repo.manager;

import java.util.Set;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;

public class UserInfoTestHelper {

	public static UserInfo createUserInfo(boolean isAdmin, Long userId, String realmId) {
		UserInfo result =new UserInfo(isAdmin, userId, realmId);
		result.setRealmAnonymousUserId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		result.setRealmPublicUsersId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		result.setRealmAuthenticatedUsersId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		return result;
	}
	
	public static UserInfo createUserInfo(boolean isAdmin, Long userId) {
		return createUserInfo(isAdmin, userId, AuthorizationConstants.DEFAULT_REALM_ID);
	}

	public static UserInfo createCertifiedUserInfo(boolean isAdmin, boolean isCertified) {
		UserInfo result =new UserInfo(isAdmin, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		result.setCertified(isCertified);
		return result;
	}
	
	public static UserInfo createAnonymousUserInfo() {
		UserInfo result = createUserInfo(false,  BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		Set<Long> groups = result.getGroups();
		// Everyone belongs to their own group and to Public
		groups.add(result.getId());
		groups.add(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		return result;
	}
}
