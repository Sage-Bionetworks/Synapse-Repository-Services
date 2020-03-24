package org.sagebionetworks.manager.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthScope;

public class UserInfoUtils {
	
	public static UserInfo createAnonymousUserInfo() {
		UserInfo result = new UserInfo(false);
		Set<Long> groups = new HashSet<Long>();
		result.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		// Everyone belongs to their own group and to Public
		groups.add(result.getId());
		groups.add(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		result.setGroups(groups);
		result.setScopes(Collections.singletonList(OAuthScope.view));
		result.setOidcClaims(Collections.EMPTY_MAP);
		return result;
	}

}
