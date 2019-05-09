package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.UnauthorizedException;

class AuthorizationStatusTest {

	AuthorizationStatus status;

	@Test
	public void testCheckAuthorizationOrElseThrow_deniedForUserNotCertified(){
		status = AuthorizationStatus.accessDenied(new UserCertificationRequiredException("user must be certified"));

		assertThrows(UserCertificationRequiredException.class, ()->{
			status.checkAuthorizationOrElseThrow();
		});
	}

	@Test
	public void testCheckAuthorizationOrElseThrow_deniedForOtherReasons(){
		status = AuthorizationStatus.accessDenied("");

		assertThrows(UnauthorizedException.class, ()->{
			status.checkAuthorizationOrElseThrow();
		});
	}

	@Test
	public void testCheckAuthorizationOrElseThrow_authorized(){
		status = AuthorizationStatus.authorized();

		assertDoesNotThrow(()->{
			status.checkAuthorizationOrElseThrow();
		});
	}
}