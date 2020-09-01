package org.sagebionetworks.repo.manager.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;

class ClaimsWithAuthTimeTest {

	@Test
	void testRoundTrip() {
		Date authDate = new Date();
		ClaimsWithAuthTime claims = ClaimsWithAuthTime.newClaims();
		claims.setAuthTime(authDate);
		// By convention the time is represented as seconds, so when we do the comparison we must truncate the milliseconds
		assertEquals(authDate.getTime()/1000L, claims.getAuthTime().getTime()/1000L);
	}

}
