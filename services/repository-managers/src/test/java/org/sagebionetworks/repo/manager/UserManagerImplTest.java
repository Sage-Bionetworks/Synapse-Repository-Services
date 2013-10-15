package org.sagebionetworks.repo.manager;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserManagerImplTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<String> groupsToDelete = null;
	
	
	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
	}

	@After
	public void tearDown() throws Exception {
		for(String groupId: groupsToDelete){
			UserGroup ug = userGroupDAO.get(groupId);
			try {
				userManager.deletePrincipal(ug.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testGetDefaultGroup() throws DatastoreException{
		// We should be able to get all default groups
		DEFAULT_GROUPS[] array = DEFAULT_GROUPS.values();
		for(DEFAULT_GROUPS group: array){
			UserGroup userGroup = userManager.getDefaultUserGroup(group);
			assertNotNull(userGroup);
		}
	}
	
	// invoke getUserInfo for Anonymous and check returned userInfo
	@Test
	public void testGetAnonymous() throws Exception {
		UserInfo ui = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		assertTrue(AuthorizationUtils.isUserAnonymous(ui));
		assertTrue(AuthorizationUtils.isUserAnonymous(ui.getIndividualGroup()));
		assertTrue(AuthorizationUtils.isUserAnonymous(ui.getUser().getUserId()));
		assertNotNull(ui.getUser().getId());
		assertEquals(2, ui.getGroups().size());
		assertTrue(ui.getGroups().contains(ui.getIndividualGroup()));
		//assertEquals(ui.getIndividualGroup(), ui.getGroups().iterator().next());
		// They belong to the public group but not the authenticated user's group
		assertTrue(ui.getGroups().contains(userGroupDAO.findGroup(AuthorizationConstants.DEFAULT_GROUPS.PUBLIC.name(), false)));
		// Anonymous does not belong to the authenticated user's group.
		assertFalse(ui.getGroups().contains(userGroupDAO.findGroup(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name(), false)));
	}
	
	@Test
	public void testStandardUser() throws Exception {
		System.out.println("Groups in the system: "+userGroupDAO.getAll());
		
		// Check that the UserInfo is populated
		UserInfo ui = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		assertNotNull(ui.getIndividualGroup());
		assertNotNull(ui.getIndividualGroup().getId());
		assertNotNull(userGroupDAO.findGroup(AuthorizationConstants.TEST_USER_NAME, true));
		
		// The group the test user belongs to should also exist
		UserGroup testGroup = userGroupDAO.findGroup(AuthorizationConstants.TEST_GROUP_NAME, false);
		assertNotNull(testGroup);
		
		// Should include Public and authenticated users' group.
		assertTrue(ui.getGroups().contains(userGroupDAO.findGroup(AuthorizationConstants.DEFAULT_GROUPS.PUBLIC.name(), false)));
		assertTrue(ui.getGroups().contains(userGroupDAO.findGroup(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name(), false)));
		
		// The group relationship should exist
		assertTrue("Missing "+testGroup+"  Has "+ui.getGroups(), ui.getGroups().contains(testGroup));
	}
		
	@Test
	public void testGetAnonymousUserInfo() throws Exception {
		userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
	}

	@Test
	public void testIdempotency() throws Exception {
		userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
	}
	
	/**
	 * This test is extremely troublesome without the TestUserDAO in place
	 * It can be reactivated once Crowd is removed and once the Named ID generator
	 * supports a one-to-many mapping
	 */
	@Ignore 
	@Test
	public void testUpdateEmail() throws Exception {
		String oldEmail = "old-change-email-test-user@sagebase.org";
		String newEmail = "new-change-email-test-user@sagebase.org";
		groupsToDelete.add(oldEmail);
		groupsToDelete.add(newEmail);
		
		// Create a user to change the email of
		userManager.createPrincipal(oldEmail, true);
		NewUser user = new NewUser();
		user.setAcceptsTermsOfUse(true);
		user.setEmail(oldEmail);
		user.setPassword("foofoobarbar");
		// userManager.createUser(user);
		
		// Make sure the new user exists
		UserInfo userInfo = userManager.getUserInfo(oldEmail);
		
		// Change the email and check it
		userManager.updateEmail(userInfo, newEmail);
		UserInfo newUserInfo = userManager.getUserInfo(newEmail);
		
		// ID should be the same after updating the email
		assertEquals(userInfo.getIndividualGroup().getId(), newUserInfo.getIndividualGroup().getId());
	}
	
	

}
