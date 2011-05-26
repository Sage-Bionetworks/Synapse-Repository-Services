package org.sagebionetworks.repo.model;

import org.junit.Test;

public class UserGroupTest {
	
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
	public void validateNotNull(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		userGroup.setName("something@somewhere.com");
		UserGroup.validate(userGroup);
	}

}
