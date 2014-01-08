package org.sagebionetworks.repo.model;

import org.junit.Test;

public class UserTest {

	@Test (expected=IllegalArgumentException.class)
	public void testValidateNull(){
		User.validateUser(null);
	}

	@Test (expected=UserNotFoundException.class)
	public void testValidateNullId(){
		User user = new User();
		User.validateUser(user);
	}

	@Test (expected=UserNotFoundException.class)
	public void testValidateNullUserId(){
		User user = new User();
		user.setId(null);
		User.validateUser(user);
	}

	@Test
	public void testValidateNotNull(){
		User user = new User();
		user.setId(new Long(123));
		user.setUserName("someUsername");
		User.validateUser(user);
	}
}
