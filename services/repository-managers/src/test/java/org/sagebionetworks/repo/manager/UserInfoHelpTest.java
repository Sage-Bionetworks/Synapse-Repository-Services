package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;

public class UserInfoHelpTest {
	
	private static UserInfo createUserInfo(Long groupId) {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setGroups(Collections.singleton(groupId));
		return userInfo;
	}

	@Test
	public void testIsCertified() {
		assertTrue(UserInfoHelper.isCertified(createUserInfo(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId())));
		
		assertFalse(UserInfoHelper.isCertified(createUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())));
	}

	@Test
	public void testIsACTMember() {
		assertTrue(UserInfoHelper.isACTMember(createUserInfo(BOOTSTRAP_PRINCIPAL.ACCESS_AND_COMPLIANCE_GROUP.getPrincipalId())));
		
		assertFalse(UserInfoHelper.isACTMember(createUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())));
	}

}
