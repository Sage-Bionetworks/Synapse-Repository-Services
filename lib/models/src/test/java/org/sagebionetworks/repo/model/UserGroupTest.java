package org.sagebionetworks.repo.model;

import org.junit.Test;
import org.sagebionetworks.repo.model.util.UserGroupUtil;

public class UserGroupTest {

	@Test (expected=IllegalArgumentException.class)
	public void validateNull(){
		UserGroupUtil.validate(null);
	}

	@Test (expected=UserNotFoundException.class)
	public void validateNullId(){
		UserGroup userGroup = new UserGroup();
		UserGroupUtil.validate(userGroup);
	}

	@Test (expected=UserNotFoundException.class)
	public void validateNullName(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		UserGroupUtil.validate(userGroup);
	}

	@Test
	public void validateValid(){
		UserGroup userGroup = new UserGroup();
		userGroup.setId("99");
		userGroup.setIsIndividual(true);
		UserGroupUtil.validate(userGroup);
	}

}
