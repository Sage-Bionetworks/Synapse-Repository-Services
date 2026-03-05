package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;

public class UserInfoHelpTest {

	@Test
	public void testIsCertified() {
		assertTrue(UserInfoHelper.isCertified(UserInfoTestHelper.createCertifiedUserInfo(false, true)));
		
		assertFalse(UserInfoHelper.isCertified(UserInfoTestHelper.createUserInfo(false, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())));
	}

	@Test
	public void testIsACTMember() {
		assertTrue(UserInfoHelper.isACTMember(UserInfoTestHelper.createUserInfo(false, BOOTSTRAP_PRINCIPAL.ACCESS_AND_COMPLIANCE_GROUP.getPrincipalId())));
		
		assertFalse(UserInfoHelper.isACTMember(UserInfoTestHelper.createUserInfo(false, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())));
	}

}
