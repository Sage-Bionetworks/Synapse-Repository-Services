package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UnauthorizedException;

public class AuthorizationManagerUtil {

	public static final AuthorizationStatus AUTHORIZED = new AuthorizationStatus(true, "");
	
	public static void checkAuthorizationAndThrowException(AuthorizationStatus auth) throws UnauthorizedException {
		if (!auth.getAuthorized()) throw new UnauthorizedException(auth.getReason());
	}

	public static AuthorizationStatus accessDenied(String reason) {
		return new AuthorizationStatus(false, reason);
	}

}
