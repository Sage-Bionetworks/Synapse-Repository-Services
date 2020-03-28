package org.sagebionetworks.manager.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.oauth.OAuthScope;

public class OAuthPermissionUtils {
	private static final Map<ACCESS_TYPE,OAuthScope> ACCESS_TYPE_TO_SCOPE;
	
	static {
		ACCESS_TYPE_TO_SCOPE = new HashMap<ACCESS_TYPE,OAuthScope>();

		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.READ, 						OAuthScope.view);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.READ_PRIVATE_SUBMISSION, 	OAuthScope.view);
		
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.CREATE, 					OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.CHANGE_PERMISSIONS, 		OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.UPDATE, 					OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.UPLOAD, 					OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.DELETE, 					OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.SUBMIT, 					OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.UPDATE_SUBMISSION, 		OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.DELETE_SUBMISSION, 		OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE, 	OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.SEND_MESSAGE, 				OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.CHANGE_SETTINGS, 			OAuthScope.modify);
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.MODERATE, 					OAuthScope.modify);
		
		ACCESS_TYPE_TO_SCOPE.put(ACCESS_TYPE.DOWNLOAD, 					OAuthScope.download);
	}
	
	public static boolean scopeAllowsAccess(Collection<OAuthScope> scopes, ACCESS_TYPE accessType) {
		OAuthScope scope = ACCESS_TYPE_TO_SCOPE.get(accessType);
		return scopes.contains(scope);
	}
	
	public static void checkScopeAllowsAccess(Collection<OAuthScope> scopes, ACCESS_TYPE accessType) {
		if (!scopeAllowsAccess(scopes, accessType)) {
			accessDenied(accessType).checkAuthorizationOrElseThrow();
		}
	}
	
	public static AuthorizationStatus accessDenied(ACCESS_TYPE accessType) {
		return AuthorizationStatus.accessDenied("Your authorization scope(s) do not allow "+accessType+" access.");			

	}
}
