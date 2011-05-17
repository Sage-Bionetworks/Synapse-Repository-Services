package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })

public class UserGroupManagerImplTest {
	
	@Autowired
	UserGroupManager userGroupManager;

	@Before
	public void setUp() throws Exception {
		assertNotNull(userGroupManager);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetAnonymousUserInfo() throws Exception {
		userGroupManager.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID);
	}

	@Test
	public void testIdempotency() throws Exception {
		userGroupManager.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID);
		userGroupManager.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID);
	}

	@Test
	public void testGetUserInfo() throws Exception {
		userGroupManager.getUserInfo("foo");
	}

}
