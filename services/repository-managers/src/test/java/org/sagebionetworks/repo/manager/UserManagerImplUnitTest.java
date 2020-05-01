package org.sagebionetworks.repo.manager;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class UserManagerImplUnitTest {
	
	private UserManager userManager;
	
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private UserProfileManager mockUserProfileManger;
	@Mock
	private GroupMembersDAO mockGroupMembersDAO;
	@Mock
	private AuthenticationDAO mockAuthDAO;
	@Mock
	private DBOBasicDao basicDAO;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	private NotificationEmailDAO notificationEmailDao;
	
	private UserInfo admin;
	private UserInfo notAdmin;
	
	private static final String MOCK_GROUP_ID = "-1";
	private UserGroup mockUserGroup;
	private String alias;
	private PrincipalAlias principalAlias;
	
	@BeforeEach
	public void setUp() throws Exception {
		mockUserGroupDAO = mock(UserGroupDAO.class);
		mockUserProfileManger = mock(UserProfileManager.class);
		mockGroupMembersDAO = mock(GroupMembersDAO.class);
		mockAuthDAO = mock(AuthenticationDAO.class);
		basicDAO = mock(DBOBasicDao.class);
		mockPrincipalAliasDAO = mock(PrincipalAliasDAO.class);

		when(mockUserGroupDAO.create(any(UserGroup.class))).thenReturn(Long.parseLong(MOCK_GROUP_ID));
		mockUserGroup = new UserGroup();
		mockUserGroup.setId(MOCK_GROUP_ID);
		mockUserGroup.setIsIndividual(true);
		when(mockUserGroupDAO.get(anyLong())).thenReturn(mockUserGroup);
				
		notificationEmailDao = Mockito.mock(NotificationEmailDAO.class);
		
		userManager = new UserManagerImpl();
		ReflectionTestUtils.setField(userManager, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(userManager, "userGroupDAO", mockUserGroupDAO);
		ReflectionTestUtils.setField(userManager, "authDAO", mockAuthDAO);
		ReflectionTestUtils.setField(userManager, "userProfileManger", mockUserProfileManger);
		ReflectionTestUtils.setField(userManager, "notificationEmailDao", notificationEmailDao);
		ReflectionTestUtils.setField(userManager, "basicDAO", basicDAO);
		ReflectionTestUtils.setField(userManager, "groupMembersDAO", mockGroupMembersDAO);
		
		admin = new UserInfo(true);
		notAdmin = new UserInfo(false);
		
		alias = "alias";
		principalAlias = new PrincipalAlias();
		principalAlias.setAlias(alias);
		principalAlias.setAliasId(3333L);
		principalAlias.setType(AliasType.USER_NAME);
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(eq(alias), any())).thenReturn(principalAlias);
	}
	
	@Test
	public void testGetUserInfo() {
		Long principalId = 111L;
		UserGroup principal = new UserGroup();
		principal.setId(principalId.toString());
		principal.setIsIndividual(true);
		when(mockUserGroupDAO.get(principalId)).thenReturn(principal);
		
		UserGroup someGroup = new UserGroup();
		someGroup.setIsIndividual(false);
		someGroup.setId("222");
		when(mockGroupMembersDAO.getUsersGroups(principalId.toString())).thenReturn(Collections.singletonList(someGroup));
		
		
		// method under test
		UserInfo userInfo = userManager.getUserInfo(principalId);
		
		assertFalse(userInfo.isAdmin());
		Set<Long> expectedUserGroupIds = new HashSet<Long>();
		expectedUserGroupIds.add(Long.parseLong(someGroup.getId()));
		expectedUserGroupIds.add(principalId);
		expectedUserGroupIds.add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		expectedUserGroupIds.add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());

		assertEquals(expectedUserGroupIds, userInfo.getGroups());
		assertEquals(principalId, userInfo.getId());
	}
	
	@Test
	public void testGetAdminUserInfo() {
		Long principalId = 111L;
		UserGroup principal = new UserGroup();
		principal.setId(principalId.toString());
		principal.setIsIndividual(true);
		when(mockUserGroupDAO.get(principalId)).thenReturn(principal);
		
		UserGroup adminGroup = new UserGroup();
		adminGroup.setIsIndividual(false);
		adminGroup.setId(TeamConstants.ADMINISTRATORS_TEAM_ID.toString());
		when(mockGroupMembersDAO.getUsersGroups(principalId.toString())).thenReturn(Collections.singletonList(adminGroup));
		
		
		// method under test
		UserInfo userInfo = userManager.getUserInfo(principalId);
		
		assertTrue(userInfo.isAdmin());
		Set<Long> expectedUserGroupIds = new HashSet<Long>();
		expectedUserGroupIds.add(Long.parseLong(adminGroup.getId()));
		expectedUserGroupIds.add(principalId);
		expectedUserGroupIds.add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		expectedUserGroupIds.add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());

		assertEquals(expectedUserGroupIds, userInfo.getGroups());
		assertEquals(principalId, userInfo.getId());
	}
	
	@Test
	public void testCreateUserAdmin() throws Exception {
		// Call with an admin
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString()+"@testing.com");
		nu.setUserName(UUID.randomUUID().toString());
		userManager.createOrGetTestUser(admin, nu, null, null);
		verify(notificationEmailDao).create((PrincipalAlias)any());
		
		// Call with a non admin
		try {
			userManager.createOrGetTestUser(notAdmin, null, null, null);
			fail();
		} catch (UnauthorizedException e) { }
		
	}
	
	@Test
	public void testGetUserAdmin() throws Exception {
		long principalId=1111L;
		PrincipalAlias alias = new PrincipalAlias();
		alias.setPrincipalId(principalId);
		// Call with an admin
		NewUser nu = new NewUser();
		String username = UUID.randomUUID().toString();
		String email = UUID.randomUUID().toString()+"@testing.com";
		nu.setUserName(username);
		nu.setEmail(email);
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(username)).thenReturn(alias);
		
		// method under test
		UserInfo userInfo = userManager.createOrGetTestUser(admin, nu, null, null);
		// we get back the principal ID for the existing user
		assertEquals(principalId, userInfo.getId().longValue());
		
		// check that a new user was never created
		verify(mockUserGroupDAO, never()).create(any());
	}
	
	@Test
	public void testDeleteUserAdmin() throws Exception {
		// Call with an admin
		userManager.deletePrincipal(admin, Long.parseLong(MOCK_GROUP_ID));
		verify(mockUserGroupDAO).delete(anyString());
		
		// Call with a non admin
		try {
			userManager.deletePrincipal(notAdmin, Long.parseLong(MOCK_GROUP_ID));
			fail();
		} catch (UnauthorizedException e) { }
	}
	
	@Test
	public void testBindAlias() throws Exception {
		String aliasName = "name";
		AliasType type = AliasType.USER_OPEN_ID;
		Long principalId = 101L;
		PrincipalAlias expected = new PrincipalAlias();
		expected.setAlias(aliasName);
		expected.setPrincipalId(principalId);
		expected.setType(type);
		when(mockPrincipalAliasDAO.bindAliasToPrincipal(eq(expected))).thenReturn(expected);
		PrincipalAlias result = userManager.bindAlias(aliasName, type, principalId);
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(eq(expected));
		assertEquals(expected, result);
	}
	
	@Test
	public void testUnBindAlias() throws Exception {
		String aliasName = "name";
		AliasType type = AliasType.USER_OPEN_ID;
		Long principalId = 101L;
		PrincipalAlias principalAlias = new PrincipalAlias();
		principalAlias.setAliasId(999L);
		principalAlias.setAlias(aliasName);
		principalAlias.setPrincipalId(principalId);
		principalAlias.setType(type);
		List<PrincipalAlias> list = Collections.singletonList(principalAlias);
		when(mockPrincipalAliasDAO.
				listPrincipalAliases(principalId, type, aliasName)).
				thenReturn(list);
		
		userManager.unbindAlias(aliasName, type, principalId);
		
		verify(mockPrincipalAliasDAO).listPrincipalAliases(principalId, type, aliasName);
		verify(mockPrincipalAliasDAO).removeAliasFromPrincipal(principalId, 999L);
	}
	
	
	@Test
	public void testGetDistinctUserIdsForAliases(){
		List<String> aliases = Lists.newArrayList("one","two");
		List<AliasType> typeFilter = Lists.newArrayList(AliasType.USER_NAME, AliasType.TEAM_NAME);
		Long limit = 100L;
		Long offset = 0L;
		List<Long> princpalIds = Lists.newArrayList(101L,102L);
		when(mockPrincipalAliasDAO.findPrincipalsWithAliases(aliases, typeFilter)).thenReturn(princpalIds);
		Set<String> principalSet = Sets.newHashSet("101", "102");
		Set<String> finalResults = Sets.newHashSet("101","103","104");
		when(mockGroupMembersDAO.getIndividuals(principalSet, limit, offset)).thenReturn(finalResults);
		
		// call under test
		Set<String> results = userManager.getDistinctUserIdsForAliases(aliases, limit, offset);
		assertNotNull(results);
		assertEquals(finalResults, results);
	}
	
	@Test
	public void testLookupUserByUsernameOrEmail() {
		// call under test
		PrincipalAlias pa = userManager.lookupUserByUsernameOrEmail(alias);
		assertEquals(principalAlias, pa);
	}
	
	@Test
	public void testLookupUserByUsernameOrEmailNotFound() {
		// unknown alias
		alias = "unknown";
		try {
			// call under test
			userManager.lookupUserByUsernameOrEmail(alias);
			fail();
		} catch (NotFoundException e) {
			assertEquals("Did not find a user with alias: unknown",e.getMessage());
		}
	}
}
