package org.sagebionetworks.repo.model;

import org.junit.Test;

public class UserInfoTest {

	@Test (expected=IllegalArgumentException.class)
	public void testValidateNull(){
		UserInfo.validateUserInfo(null);
	}

}
