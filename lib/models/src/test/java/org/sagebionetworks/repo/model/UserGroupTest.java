package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class UserGroupTest {
	
	@Test
	public  void testIsEmailAddress(){
		assertTrue(UserGroup.isEmailAddress("something@gmail.com"));
		assertFalse(UserGroup.isEmailAddress("PUBLIC"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void validateNull(){
		UserGroup.validate(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void validateNullId(){
		UserGroup userGroup = new UserGroup();
		UserGroup.validate(userGroup);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void validateNullName(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		UserGroup.validate(userGroup);
	}
	
	@Test
	public void validateValid(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		userGroup.setName("something@somewhere.com");
		userGroup.setIndividual(true);
		UserGroup.validate(userGroup);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void validateGroupWithEmailAddressName(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		userGroup.setName("something@somewhere.com");
		userGroup.setIndividual(false);
		UserGroup.validate(userGroup);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void validateUserWithoutEmailAddressName(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		userGroup.setName("someName");
		userGroup.setIndividual(true);
		UserGroup.validate(userGroup);
	}

}
