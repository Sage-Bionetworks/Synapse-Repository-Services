package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UserInfoTest {

	@Test (expected=IllegalArgumentException.class)
	public void testValidateNull(){
		UserInfo.validateUserInfo(null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateNullUser(){
		UserInfo info = new UserInfo(false);
		UserInfo.validateUserInfo(info);
	}

	@Test (expected=UserNotFoundException.class)
	public void testValidateNullUserId(){
		UserInfo info = new UserInfo(false);
		User user = new User();
		info.setUser(user);
		UserInfo.validateUserInfo(info);
	}

	@Test (expected=UserNotFoundException.class)
	public void testValidateNullUserUserId(){
		UserInfo info = new UserInfo(false);
		User user = new User();
		user.setId(new Long(101));
		user.setUserName("myUserName");
		info.setUser(user);
		UserGroup ind = new UserGroup();
		ind.setId("9");
		info.setIndividualGroup(ind);
		List<UserGroup> groups = new ArrayList<UserGroup>();
		// This will have null values
		groups.add(new UserGroup());
		info.setGroups(groups);
		UserInfo.validateUserInfo(info);
	}

	@Test
	public void testValidateValid(){
		UserInfo info = new UserInfo(false);
		User user = new User();
		user.setId(new Long(101));
		user.setUserName("myUserName");
		info.setUser(user);
		UserGroup ind = new UserGroup();
		ind.setId("9");
		ind.setIsIndividual(false);
		info.setIndividualGroup(ind);
		List<UserGroup> groups = new ArrayList<UserGroup>();
		// This will have null values
		UserGroup group = new UserGroup();
		group.setId("0");
		group.setIsIndividual(false);
		groups.add(group);
		info.setGroups(groups);
		UserInfo.validateUserInfo(info);
	}
}
