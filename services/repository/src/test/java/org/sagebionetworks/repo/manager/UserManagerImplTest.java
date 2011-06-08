package org.sagebionetworks.repo.manager;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class UserManagerImplTest {
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	UserGroupDAO userGroupDAO;
	
	private static final String TEST_USER = "test-user";
	
	private void cleanUpGroups() throws Exception {
		
		for (UserGroup g: userGroupDAO.getAll(true)) {
			if (g.getName().equals(AuthorizationConstants.ADMIN_GROUP_NAME)) {
				// leave it
			} else if (g.getName().equals(AuthorizationConstants.PUBLIC_GROUP_NAME)) {
				// leave it
			} else if (g.getName().equals(AuthorizationConstants.ANONYMOUS_USER_ID)) {
				// leave it
			} else {
//				System.out.println("Deleting: "+g);
				userGroupDAO.delete(g.getId());
			}
		}

	}
	
	@Before
	public void setUp() throws Exception {
		cleanUpGroups();
		userManager.setUserDAO(new TestUserDAO());
	}

	@After
	public void tearDown() throws Exception {
		cleanUpGroups();
	}
	
	// invoke getUserInfo for Anonymous and check returned userInfo
	@Test
	public void testGetAnonymous() throws Exception {
		UserInfo ui = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		assertEquals(AuthorizationConstants.ANONYMOUS_USER_ID, ui.getUser().getUserId());
		assertNotNull(ui.getUser().getId());
		assertEquals(AuthorizationConstants.ANONYMOUS_USER_ID, ui.getIndividualGroup().getName());
		assertEquals(1, ui.getGroups().size());
		assertEquals(ui.getIndividualGroup(), ui.getGroups().iterator().next());
		// does not include Public group
		assertFalse(ui.getGroups().contains(userGroupDAO.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false)));
	}
	
	@Test
	public void testStandardUser() throws Exception {
		System.out.println("Groups in the system: "+userGroupDAO.getAll());
		// call for a user in the User system but not the Permissions system.  
		assertNull(userGroupDAO.findGroup(TEST_USER, true));
		UserInfo ui = userManager.getUserInfo(TEST_USER);
		//		check the returned userInfo
		//		verify the user gets created
		assertNotNull(userGroupDAO.findGroup(TEST_USER, true));
		//		should include Public group
		assertTrue(ui.getGroups().contains(userGroupDAO.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false)));
		// call for a user in a Group not in the Permissions system
		//		verify the group is created in the Permissions system
		assertTrue(ui.getGroups().contains(userGroupDAO.findGroup(TestUserDAO.TEST_GROUP_NAME, false)));
	}
		
	@Test
	public void testGetAnonymousUserInfo() throws Exception {
		userManager.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID);
	}

	@Test
	public void testIdempotency() throws Exception {
		userManager.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID);
		userManager.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID);
	}

}
