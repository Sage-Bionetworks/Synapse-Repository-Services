package org.sagebionetworks.repo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.Test;
import org.sagebionetworks.repo.model.auth.CallersContext;
import org.sagebionetworks.repo.model.auth.IdentityProviderName;

public class UserInfoTest {

	@Test
	public void testValidateNull(){
		assertThrows(IllegalArgumentException.class, () -> UserInfo.validateUserInfo(null));
	}

	@Test
	public void testConstructorWithNullId(){
		// call under test
		assertThrows(IllegalArgumentException.class, () -> new UserInfo(false, null, "0"));
	}

	@Test
	public void testConstructorWithNullRealmId(){
		// call under test
		assertThrows(IllegalArgumentException.class, () -> new UserInfo(false, 101L, null));
	}

	@Test
	public void testConstructorWithNullGroups(){
		// call under test
		assertThrows(IllegalArgumentException.class, () -> new UserInfo(false, 101L, "0", null));
	}

	@Test
	public void testIsAnonymous() {
		Long userId = 101L;
		UserInfo userInfo = new UserInfo(false, userId, "0");
		
		// method under test
		assertFalse(userInfo.isUserAnonymous());
		
		Long anonId = 202L;
		userInfo.setRealmAnonymousUserId(anonId);
		
		// method under test
		assertFalse(userInfo.isUserAnonymous());
		
		userInfo.setRealmAnonymousUserId(userId);
		
		// method under test
		assertTrue(userInfo.isUserAnonymous());
		
		
	}

	@Test
	public void testGetAuthenticatingIdentityProvider() {
		UserInfo userInfo = new UserInfo(false, 101L, "0");
		userInfo.setContext(new CallersContext().setIdentityProvider(IdentityProviderName.ORCID));

		// method under test
		assertEquals(Optional.of(IdentityProviderName.ORCID), userInfo.getAuthenticatingIdentityProvider());
	}

	@Test
	public void testGetAuthenticatingIdentityProviderWithNoIdentityProvider() {
		UserInfo userInfo = new UserInfo(false, 101L, "0");
		userInfo.setContext(new CallersContext().setSessionId("abc"));

		// method under test
		assertEquals(Optional.empty(), userInfo.getAuthenticatingIdentityProvider());
	}

	@Test
	public void testGetAuthenticatingIdentityProviderWithNoContext() {
		UserInfo userInfo = new UserInfo(false, 101L, "0");

		// method under test
		assertEquals(Optional.empty(), userInfo.getAuthenticatingIdentityProvider());
	}

}
