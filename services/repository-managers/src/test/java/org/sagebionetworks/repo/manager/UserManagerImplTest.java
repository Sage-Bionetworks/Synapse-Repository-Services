package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserManagerImplTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthenticationDAO authDAO;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
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
		assertTrue(AuthorizationUtils.isUserAnonymous(ui.getId()));
		assertTrue(AuthorizationUtils.isUserAnonymous(Long.parseLong(ui.getId().toString())));
		assertNotNull(ui.getId());
		assertEquals(2, ui.getGroups().size());
		assertTrue(ui.getGroups().contains(ui.getId()));

		// They belong to the public group but not the authenticated user's group
		assertTrue(ui.getGroups().contains(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()));

		// Anonymous does not belong to the authenticated user's group.
		assertFalse(ui.getGroups().contains(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId()));
		assertFalse(ui.acceptsTermsOfUse());
	}
	
	@Test
	public void testStandardUser() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		Long principalId = userManager.createUser(user);;
		groupsToDelete.add(principalId.toString());
		
		// Check that the UserInfo is populated
		UserInfo ui = userManager.getUserInfo(principalId);
		assertNotNull(ui.getId().toString());
		
		// check aliases
		List<PrincipalAlias> aliases = principalAliasDAO.listPrincipalAliases(principalId);
		assertEquals(2, aliases.size());
		for (PrincipalAlias alias : aliases) {
			if (alias.getType().equals(AliasType.USER_NAME)) {
				assertEquals(user.getUserName(), alias.getAlias());
			} else if (alias.getType().equals(AliasType.USER_EMAIL)) {
				assertEquals(user.getEmail(), alias.getAlias());
			} else {
				fail("Unexpected alias type "+alias.getType());
			}
		}
		
		// Should include Public and authenticated users' group.
		assertTrue(ui.getGroups().contains(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()));
		assertTrue(ui.getGroups().contains(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId()));
		assertFalse(ui.acceptsTermsOfUse());
	}
	
	@Test
	public void testGetUserAcceptsTermsOfUseWithNotSet() {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		Long principalId = userManager.createUser(user);;
		groupsToDelete.add(principalId.toString());
		
		// Check that the UserInfo is populated
		UserInfo userInfo = userManager.getUserInfo(principalId);
		assertEquals(principalId, userInfo.getId());
		assertFalse(userInfo.acceptsTermsOfUse());
	}
	
	@Test
	public void testGetUserAcceptsTermsOfUseWithTrue() {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		Long principalId = userManager.createUser(user);;
		groupsToDelete.add(principalId.toString());
		
		authDAO.setTermsOfUseAcceptance(principalId, true);
		
		// Check that the UserInfo is populated
		UserInfo userInfo = userManager.getUserInfo(principalId);
		assertEquals(principalId, userInfo.getId());
		assertTrue(userInfo.acceptsTermsOfUse());
	}
	
	@Test
	public void testGetUserAcceptsTermsOfUseWithFalse() {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		Long principalId = userManager.createUser(user);;
		groupsToDelete.add(principalId.toString());
		
		authDAO.setTermsOfUseAcceptance(principalId, false);
		
		// Check that the UserInfo is populated
		UserInfo userInfo = userManager.getUserInfo(principalId);
		assertEquals(principalId, userInfo.getId());
		assertFalse(userInfo.acceptsTermsOfUse());
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
}
