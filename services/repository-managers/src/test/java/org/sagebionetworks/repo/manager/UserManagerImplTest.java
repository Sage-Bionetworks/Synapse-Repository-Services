package org.sagebionetworks.repo.manager;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
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
	
	private List<String> groupsToDelete;
	
	
	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
	}

	@After
	public void tearDown() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		for (String groupId : groupsToDelete) {
			userManager.deletePrincipal(adminUserInfo, Long.parseLong(groupId));
		}
	}
	
	@Test
	public void testGetAnonymous() throws Exception {
		UserInfo ui = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		assertTrue(AuthorizationUtils.isUserAnonymous(ui));
		assertTrue(AuthorizationUtils.isUserAnonymous(ui.getIndividualGroup()));
		assertTrue(AuthorizationUtils.isUserAnonymous(ui.getIndividualGroup().getId()));
		assertNotNull(ui.getUser().getId());
		assertEquals(2, ui.getGroups().size());
		assertTrue(ui.getGroups().contains(ui.getIndividualGroup()));

		// They belong to the public group but not the authenticated user's group
		assertTrue(ui.getGroups().contains(userGroupDAO.get(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().toString())));

		// Anonymous does not belong to the authenticated user's group.
		assertFalse(ui.getGroups().contains(userGroupDAO.get(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString())));
	}
	
	@Test
	public void testStandardUser() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@");
		Long principalId = userManager.createUser(user);
		groupsToDelete.add(principalId.toString());
		
		// Check that the UserInfo is populated
		UserInfo ui = userManager.getUserInfo(principalId);
		assertNotNull(ui.getIndividualGroup());
		assertNotNull(ui.getIndividualGroup().getId());
		
		// Should include Public and authenticated users' group.
		assertTrue(ui.getGroups().contains(userGroupDAO.get(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().toString())));
		assertTrue(ui.getGroups().contains(userGroupDAO.get(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString())));
	}
		
	@Test
	public void testGetAnonymousUserInfo() throws Exception {
		userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
	}

	@Test
	public void testIdempotency() throws Exception {
		userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
	}
	
	/**
	 * This test can be reactivated once the Named ID generator
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
		NewUser oldie = new NewUser();
		oldie.setEmail(oldEmail);
		oldie.setPassword("foobar");
		userManager.createUser(oldie);
		
		// Make sure the new user exists
		UserInfo userInfo = userManager.getUserInfo(oldEmail);
		
		// Change the email and check it
		userManager.updateEmail(userInfo, newEmail);
		UserInfo newUserInfo = userManager.getUserInfo(newEmail);
		
		// ID should be the same after updating the email
		assertEquals(userInfo.getIndividualGroup().getId(), newUserInfo.getIndividualGroup().getId());
	}
}
