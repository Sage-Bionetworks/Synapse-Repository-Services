package org.sagebionetworks.manager.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.util.ValidateArgument;

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
		ValidateArgument.required(scopes, "scopes");
		if (!ACCESS_TYPE_TO_SCOPE.containsKey(accessType)) {
			throw new RuntimeException("Acess type "+accessType+" must be mapped to some OAuth scope.");
		}
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
	
	public static String scopeDescription(OAuthScope scope) {
		switch (scope) {
		case openid:
			return "To see your identity";
		case view:
			return "To view the content which you can view";
		case modify:
			return "To modify the content which you can modify (create, change, delete)";
		case download:
			return "To download the content which you can download";				
		}
		return null;
	}
}
