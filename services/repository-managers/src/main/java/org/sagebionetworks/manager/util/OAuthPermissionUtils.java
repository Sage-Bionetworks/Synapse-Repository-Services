package org.sagebionetworks.manager.util;

import java.util.Collection;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.oauth.OAuthScope;

import com.google.common.collect.ImmutableSet;

public class OAuthPermissionUtils {
	private static final Set<ACCESS_TYPE> VIEWING_ACCESS_TYPES = ImmutableSet.of(
			ACCESS_TYPE.READ,
			ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
	
	private static final Set<ACCESS_TYPE> MUTATING_ACCESS_TYPES = ImmutableSet.of(
			ACCESS_TYPE.CREATE,
			ACCESS_TYPE.CHANGE_PERMISSIONS,
			ACCESS_TYPE.UPDATE,
			ACCESS_TYPE.UPLOAD,
			ACCESS_TYPE.DELETE,
			ACCESS_TYPE.SUBMIT,
			ACCESS_TYPE.UPDATE_SUBMISSION,
			ACCESS_TYPE.DELETE_SUBMISSION,
			ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE,
			ACCESS_TYPE.SEND_MESSAGE,
			ACCESS_TYPE.CHANGE_SETTINGS,
			ACCESS_TYPE.MODERATE);
	
	private static final Set<ACCESS_TYPE> DOWNLOADING_ACCESS_TYPES = ImmutableSet.of(
			ACCESS_TYPE.DOWNLOAD);
	
	public static boolean scopeAllowsAccess(Collection<OAuthScope> scopes, ACCESS_TYPE accessType) {
		for (OAuthScope scope : scopes) {
			switch (scope) {
			case view:
				return VIEWING_ACCESS_TYPES.contains(accessType);
			case download:
				return DOWNLOADING_ACCESS_TYPES.contains(accessType);
			case modify:
				return MUTATING_ACCESS_TYPES.contains(accessType);
			default:
				continue;
			}
		}
		return false;
	}
	
	public static AuthorizationStatus accessDenied(ACCESS_TYPE accessType) {
		return AuthorizationStatus.accessDenied("Your authorization scope(s) do not allow "+accessType+" access.");			

	}
}
