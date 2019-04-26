package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.UnauthorizedException;

class AuthorizationManagerUtilTest {

	AuthorizationStatus status;

	@Test
	public void testCheckAuthorizationAndThrowException_deniedForUserNotCertified(){
		status = new AuthorizationStatus(false,"", AuthorizationStatusDenialReason.USER_NOT_CERTIFIED);

		assertThrows(UserCertificationRequiredException.class, ()->{
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(status);
		});
	}

	@Test
	public void testCheckAuthorizationAndThrowException_deniedForOtherReasons(){
		status = new AuthorizationStatus(false,"");

		assertThrows(UnauthorizedException.class, ()->{
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(status);
		});
	}

	@Test
	public void testCheckAuthorizationAndThrowException_authorized(){
		status = new AuthorizationStatus(true,"");

		assertDoesNotThrow(()->{
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(status);
		});
	}
}