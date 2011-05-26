package org.sagebionetworks.repo.model;

import org.junit.Test;

public class UserTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateNull(){
		User.validateUser(null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateNullId(){
		User user = new User();
		User.validateUser(user);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateNullUserId(){
		User user = new User();
		user.setId("1010");
		User.validateUser(user);
	}

	
	@Test
	public void testValidateNotNull(){
		User user = new User();
		user.setId("1010");
		user.setUserId("myname@gmail.com");
		User.validateUser(user);
	}
}
