package org.sagebionetworks.repo.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

public class UserInfoTest {

	@Test (expected=IllegalArgumentException.class)
	public void testValidateNull(){
		UserInfo.validateUserInfo(null);
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

}
