package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;

public class AuthorizationUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testIsCertifiedUser() {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setGroups(new HashSet<Long>());
		assertFalse(AuthorizationUtils.isCertifiedUser(userInfo));
		userInfo.getGroups().add(
				BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		assertTrue(AuthorizationUtils.isCertifiedUser(userInfo));
	}

	@Test
	public void testIsCertifiedUserAdmin() {
		UserInfo userInfo = new UserInfo(true);
		userInfo.setGroups(new HashSet<Long>());
		assertTrue(AuthorizationUtils.isCertifiedUser(userInfo));
	}
}
