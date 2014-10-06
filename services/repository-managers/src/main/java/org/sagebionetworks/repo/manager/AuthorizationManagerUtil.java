package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.util.Pair;

public class AuthorizationManagerUtil {

	public static final Pair<Boolean,String> AUTHORIZED = new Pair<Boolean,String>(true, "");
	
	public static void checkAuthorizationAndThrowException(Pair<Boolean,String> auth) throws UnauthorizedException {
		if (!auth.getFirst()) throw new UnauthorizedException(auth.getSecond());
	}

	public static Pair<Boolean,String> accessDenied(String reason) {
		return new Pair<Boolean,String>(false, reason);
	}

}
