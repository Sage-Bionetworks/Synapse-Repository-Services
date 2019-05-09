package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

	@Test
	public void testEquals_authorized(){
		assertEquals(AuthorizationStatus.authorized(), AuthorizationStatus.authorized());
	}

	@Test
	public void testEquals_denied_sameMessage_sameException(){
		String messsage = "testerino";

		assertEquals(AuthorizationStatus.accessDenied(new IllegalArgumentException(messsage)), AuthorizationStatus.accessDenied(new IllegalArgumentException(messsage)));
	}

	@Test
	public void testEquals_denied_sameMessage_diffException(){
		String messsage = "updog";

		assertNotEquals(AuthorizationStatus.accessDenied(new IllegalArgumentException(messsage)), AuthorizationStatus.accessDenied(new UnauthorizedException(messsage)));
	}


	@Test
	public void testEquals_denied_diffMessage_diffException(){
		assertNotEquals(AuthorizationStatus.accessDenied(new IllegalArgumentException("asdf")), AuthorizationStatus.accessDenied(new UnauthorizedException("qwerty")));
	}

	@Test
	public void testEquals_denied_diffMessage_sameException(){
		assertNotEquals(AuthorizationStatus.accessDenied(new IllegalArgumentException("asdf")), AuthorizationStatus.accessDenied(new IllegalArgumentException("qwerty")));
	}
}