package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.util.UserGroupUtil;

public class UserGroupTest {
	
	@Test
	public  void testIsEmailAddress(){
		assertTrue(UserGroupUtil.isEmailAddress("something@gmail.com"));
		assertFalse(UserGroupUtil.isEmailAddress("PUBLIC"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void validateNull(){
		UserGroupUtil.validate(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void validateNullId(){
		UserGroup userGroup = new UserGroup();
		UserGroupUtil.validate(userGroup);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void validateNullName(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		UserGroupUtil.validate(userGroup);
	}
	
	@Test
	public void validateValid(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		userGroup.setName("something@somewhere.com");
		userGroup.setIsIndividual(true);
		UserGroupUtil.validate(userGroup);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void validateGroupWithEmailAddressName(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		userGroup.setName("something@somewhere.com");
		userGroup.setIsIndividual(false);
		UserGroupUtil.validate(userGroup);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void validateUserWithoutEmailAddressName(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		userGroup.setName("someName");
		userGroup.setIsIndividual(true);
		UserGroupUtil.validate(userGroup);
	}

}
