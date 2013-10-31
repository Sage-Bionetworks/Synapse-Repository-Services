package org.sagebionetworks.repo.model.util;

import java.util.ArrayList;

import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;

/**
 * Creates a stubbed user Info for test.
 * @author jmhill
 *
 */
public class UserInfoUtils {
	
	public static UserInfo createValidUserInfo(boolean isAdmin){
		User user = new User();
		user.setId("23");
		user.setUserId("someTestUser@gmail.com");
		UserGroup group = new UserGroup();
		group.setId("3");
		group.setName("foo@bar.com");
		group.setIsIndividual(true);
		UserInfo info = new UserInfo(isAdmin);
		info.setUser(user);
		info.setIndividualGroup(group);
		info.setGroups(new ArrayList<UserGroup>());
		UserInfo.validateUserInfo(info);
		return info;
	}

}
