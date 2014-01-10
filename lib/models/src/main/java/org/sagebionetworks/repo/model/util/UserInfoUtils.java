package org.sagebionetworks.repo.model.util;

import org.sagebionetworks.repo.model.UserInfo;

/**
 * Creates a stubbed user Info for test.
 * @author jmhill
 *
 */
public class UserInfoUtils {
	
	public static UserInfo createValidUserInfo(boolean isAdmin){
		return new UserInfo(isAdmin, 123L);
	}

}
