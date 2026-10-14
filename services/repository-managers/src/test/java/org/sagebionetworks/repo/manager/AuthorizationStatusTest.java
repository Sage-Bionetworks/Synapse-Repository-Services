package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;

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
	
	@Test
	public void testAccessDeniedButAggregateAllowed(){
		String message = "aggregate only";
		String aggregateDataSourceId = "syn123";

		// call under test
		status = AuthorizationStatus.accessDeniedButAggregateAllowed(message, aggregateDataSourceId);

		assertFalse(status.isAuthorized());
		assertTrue(status.isAggregateAccessAllowed());
		assertEquals(Optional.of(aggregateDataSourceId), status.getAggregateDataSourceId());
		assertEquals(message, status.getMessage());
		assertThrows(UnauthorizedException.class, ()->{
			status.checkAuthorizationOrElseThrow();
		});
	}

	@Test
	public void testIsAggregateAccessAllowedWithAuthorized(){
		status = AuthorizationStatus.authorized();

		assertFalse(status.isAggregateAccessAllowed());
	}

	@Test
	public void testIsAggregateAccessAllowedWithAccessDenied(){
		status = AuthorizationStatus.accessDenied("nope");

		assertFalse(status.isAggregateAccessAllowed());
		assertEquals(Optional.empty(), status.getAggregateDataSourceId());
	}

	@Test
	public void testEquals_denied_sameMessage_differByAggregateAllowed(){
		String message = "same message";

		assertNotEquals(AuthorizationStatus.accessDenied(message),
				AuthorizationStatus.accessDeniedButAggregateAllowed(message, "syn123"));
	}

	@Test
	public void testEquals_deniedAggregateAllowed_differBySourceId(){
		String message = "same message";

		assertNotEquals(AuthorizationStatus.accessDeniedButAggregateAllowed(message, "syn123"),
				AuthorizationStatus.accessDeniedButAggregateAllowed(message, "syn456"));
	}

	@Test
	public void testEquals_deniedAggregateAllowed_sameMessageAndSourceId(){
		String message = "same message";

		assertEquals(AuthorizationStatus.accessDeniedButAggregateAllowed(message, "syn123"),
				AuthorizationStatus.accessDeniedButAggregateAllowed(message, "syn123"));
	}

	@Test
	public void testIsAuthorizedOrElseGetWithAutorized() {
		AuthorizationStatus status = AuthorizationStatus.authorized();
		
		// Call under test
		AuthorizationStatus result = status.isAuthorizedOrElseGet(() -> "Nope");
		
		assertEquals(status, result);
	}
	
	@Test
	public void testIsAuthorizedOrElseGetWithNotAutorized() {
		AuthorizationStatus status = AuthorizationStatus.accessDenied("Nope");
		
		// Call under test
		AuthorizationStatus result = status.isAuthorizedOrElseGet(() -> "Nope override");
		
		assertNotEquals(status, result);
		assertFalse(result.isAuthorized());
		assertEquals("Nope override", result.getMessage());
	}
}