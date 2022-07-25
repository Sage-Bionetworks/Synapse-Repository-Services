package org.sagebionetworks.repo.manager;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalOIDCBindingDao;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class UserManagerImplUnitTest {

	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private UserProfileDAO userProfileDAO;
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
	@Mock
	private PrincipalOIDCBindingDao mockPrincipalOIDCDao;
	
	@InjectMocks
	private UserManagerImpl userManager;
	
	
	private UserInfo admin;
	private UserInfo notAdmin;
	
	private static final String MOCK_GROUP_ID = "-1";
	@Mock
	private UserGroup mockUserGroup;
	private String alias;
	private PrincipalAlias principalAlias;
	
	@BeforeEach
	public void setUp() throws Exception {
		admin = new UserInfo(true);
		notAdmin = new UserInfo(false);
		
		alias = "alias";
		principalAlias = new PrincipalAlias();
		principalAlias.setAlias(alias);
		principalAlias.setAliasId(3333L);
		principalAlias.setType(AliasType.USER_NAME);
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
		// Call with a non admin
		try {
			userManager.createOrGetTestUser(notAdmin, null, null, null);
			fail();
		} catch (UnauthorizedException e) { }
		
	}
	
	@Test
	public void testGetUserAdmin() throws Exception {
		long principalId=1111L;
		when(mockUserGroup.getId()).thenReturn(""+principalId);
		when(mockUserGroup.getIsIndividual()).thenReturn(true);
		when(mockUserGroupDAO.get(any(Long.class))).thenReturn(mockUserGroup);
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
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(eq(alias), any())).thenReturn(principalAlias);
		// call under test
		PrincipalAlias pa = userManager.lookupUserByUsernameOrEmail(alias);
		assertEquals(principalAlias, pa);
	}
	
	@Test
	public void testLookupUserByUsernameOrEmailNotFound() {
		// unknown alias
		alias = "unknown";
		
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			userManager.lookupUserByUsernameOrEmail(alias);
		}).getMessage();
		
		assertEquals("Did not find a user with alias: unknown", message);
	}
	
	@Test
	public void testLookupUserIdByOIDCSubject() {
		
		Optional<Long> expected = Optional.of(123L);
		
		when(mockPrincipalOIDCDao.findBindingForSubject(any(), any())).thenReturn(expected);
		
		// call under test
		Optional<Long> result = userManager.lookupUserIdByOIDCSubject(OAuthProvider.GOOGLE_OAUTH_2_0, alias);
		
		assertEquals(expected, result);
		
		verify(mockPrincipalOIDCDao).findBindingForSubject(OAuthProvider.GOOGLE_OAUTH_2_0, alias);
	}
	
	@Test
	public void testLookupUserIdByOIDCSubjectWithNoProvider() {
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			userManager.lookupUserIdByOIDCSubject(null, alias);
		}).getMessage();
		
		assertEquals("The provider is required.", message);
	}
	
	@Test
	public void testLookupUserIdByOIDCSubjectWithNoSubject() {
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			userManager.lookupUserIdByOIDCSubject(OAuthProvider.GOOGLE_OAUTH_2_0, null);
		}).getMessage();
		
		assertEquals("The subject is required and must not be the empty string.", message);
		
	}
	
	@Test
	public void testBindOauthProviderAlias() {
		
		// Call under test
		userManager.bindUserToOIDCSubject(123L, OAuthProvider.GOOGLE_OAUTH_2_0, alias);
		
		verify(mockPrincipalOIDCDao).bindPrincipalToSubject(123L, OAuthProvider.GOOGLE_OAUTH_2_0, alias);
		
	}
	
	@Test
	public void testBindOauthProviderAliasWithNoProvider() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			userManager.bindUserToOIDCSubject(123L, null, alias);
		}).getMessage();
		
		assertEquals("The provider is required.", message);
		
	}
	
	@Test
	public void testBindOauthProviderAliasWithNoSubject() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			userManager.bindUserToOIDCSubject(123L, OAuthProvider.GOOGLE_OAUTH_2_0, null);
		}).getMessage();
		
		assertEquals("The subject is required and must not be the empty string.", message);		
	}
	
	@Test
	public void testBindOauthProviderAliasWithNoUserId() {
				
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			userManager.bindUserToOIDCSubject(null, OAuthProvider.GOOGLE_OAUTH_2_0, alias);
		}).getMessage();
		
		assertEquals("The userId is required.", message);
	}
	
	@Test
	public void testCreateUser() {
		Long userId = 123L;
		
		when(mockUserGroupDAO.create(any())).thenReturn(userId);
		when(mockPrincipalAliasDAO.bindAliasToPrincipal(any())).thenReturn(principalAlias);
		
		NewUser user = new NewUser()
			.setUserName(UUID.randomUUID().toString())
			.setEmail(UUID.randomUUID().toString());
		
		// Call under test
		Long result = userManager.createUser(user);
		
		assertEquals(userId, result);
		
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(user.getUserName());
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(user.getEmail());
		
		UserGroup expectedGroup = new UserGroup()
			.setIsIndividual(true);
		
		ArgumentCaptor<UserGroup> ugCaptor = ArgumentCaptor.forClass(UserGroup.class);
		
		verify(mockUserGroupDAO).create(ugCaptor.capture());
		
		assertEquals(expectedGroup.setCreationDate(ugCaptor.getValue().getCreationDate()), ugCaptor.getValue());
		
		verify(mockAuthDAO).createNew(userId);
		
		UserProfile expectedProfile = new UserProfile()
			.setOwnerId(userId.toString())
			.setUserName(user.getUserName());
		
		verify(userProfileDAO).create(expectedProfile);
		
		PrincipalAlias usernameAlias = new PrincipalAlias()
			.setType(AliasType.USER_NAME)
			.setAlias(user.getUserName())
			.setPrincipalId(userId);
		
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(usernameAlias);
		
		PrincipalAlias emailAlias = new PrincipalAlias()
			.setType(AliasType.USER_EMAIL)
			.setAlias(user.getEmail())
			.setPrincipalId(userId);
		
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(emailAlias);
		verify(notificationEmailDao).create(principalAlias);
		verifyNoMoreInteractions(mockPrincipalAliasDAO);
		verifyNoMoreInteractions(mockPrincipalOIDCDao);
	}
	
	@Test
	public void testCreateUserWithExistingEmail() {
		NewUser user = new NewUser()
			.setUserName(UUID.randomUUID().toString())
			.setEmail(UUID.randomUUID().toString());
		
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(user.getEmail())).thenReturn(principalAlias);
		
		String result = assertThrows(NameConflictException.class, () -> {			
			// Call under test
			userManager.createUser(user);
		}).getMessage();
		
		assertEquals("User '" + user.getEmail() + "' already exists", result);
		
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(user.getEmail());
		verifyNoMoreInteractions(mockPrincipalAliasDAO);
		
		verifyZeroInteractions(mockPrincipalOIDCDao);
		verifyZeroInteractions(mockUserGroupDAO);
		verifyZeroInteractions(mockAuthDAO);
		verifyZeroInteractions(userProfileDAO);
		verifyZeroInteractions(notificationEmailDao);
	}
	
	@Test
	public void testCreateUserWithExistingUsername() {
		NewUser user = new NewUser()
			.setUserName(UUID.randomUUID().toString())
			.setEmail(UUID.randomUUID().toString());
		
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(user.getEmail())).thenReturn(null);
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(user.getUserName())).thenReturn(principalAlias);
		
		String result = assertThrows(NameConflictException.class, () -> {			
			// Call under test
			userManager.createUser(user);
		}).getMessage();
		
		assertEquals("User '" + user.getUserName() + "' already exists", result);
		
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(user.getEmail());
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(user.getUserName());
		
		verifyNoMoreInteractions(mockPrincipalAliasDAO);
		verifyZeroInteractions(mockPrincipalOIDCDao);
		verifyZeroInteractions(mockUserGroupDAO);
		verifyZeroInteractions(mockAuthDAO);
		verifyZeroInteractions(userProfileDAO);
		verifyZeroInteractions(notificationEmailDao);
	}
	
	@Test
	public void testCreateUserWithExistingOIDCBinding() {
		NewUser user = new NewUser()
			.setUserName(UUID.randomUUID().toString())
			.setEmail(UUID.randomUUID().toString())
			.setOauthProvider(OAuthProvider.GOOGLE_OAUTH_2_0)
			.setSubject(UUID.randomUUID().toString());
		
		when(mockPrincipalOIDCDao.findBindingForSubject(any(), any())).thenReturn(Optional.of(123L));
		
		String result = assertThrows(NameConflictException.class, () -> {			
			// Call under test
			userManager.createUser(user);
		}).getMessage();
		
		assertEquals("The provided '" + OAuthProvider.GOOGLE_OAUTH_2_0 + "' account is already registered with Synapse", result);
		
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(user.getEmail());
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(user.getUserName());
		verify(mockPrincipalOIDCDao).findBindingForSubject(user.getOauthProvider(), user.getSubject());
		
		verifyNoMoreInteractions(mockPrincipalAliasDAO);
		verifyZeroInteractions(mockPrincipalOIDCDao);
		verifyZeroInteractions(mockUserGroupDAO);
		verifyZeroInteractions(mockAuthDAO);
		verifyZeroInteractions(userProfileDAO);
		verifyZeroInteractions(notificationEmailDao);
	}
	
	@Test
	public void testCreateUserWithOauthProvider() {
		Long userId = 123L;
		
		when(mockUserGroupDAO.create(any())).thenReturn(userId);
		when(mockPrincipalAliasDAO.bindAliasToPrincipal(any())).thenReturn(principalAlias);
		
		NewUser user = new NewUser()
			.setUserName(UUID.randomUUID().toString())
			.setEmail(UUID.randomUUID().toString())
			.setOauthProvider(OAuthProvider.GOOGLE_OAUTH_2_0)
			.setSubject(UUID.randomUUID().toString());
		
		// Call under test
		Long result = userManager.createUser(user);
		
		assertEquals(userId, result);
		
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(user.getUserName());
		verify(mockPrincipalAliasDAO).findPrincipalWithAlias(user.getEmail());
		verify(mockPrincipalOIDCDao).findBindingForSubject(user.getOauthProvider(), user.getSubject());
		
		UserGroup expectedGroup = new UserGroup()
			.setIsIndividual(true);
		
		ArgumentCaptor<UserGroup> ugCaptor = ArgumentCaptor.forClass(UserGroup.class);
		
		verify(mockUserGroupDAO).create(ugCaptor.capture());
		
		assertEquals(expectedGroup.setCreationDate(ugCaptor.getValue().getCreationDate()), ugCaptor.getValue());
		
		verify(mockAuthDAO).createNew(userId);
		
		UserProfile expectedProfile = new UserProfile()
			.setOwnerId(userId.toString())
			.setUserName(user.getUserName());
		
		verify(userProfileDAO).create(expectedProfile);
		
		PrincipalAlias usernameAlias = new PrincipalAlias()
			.setType(AliasType.USER_NAME)
			.setAlias(user.getUserName())
			.setPrincipalId(userId);
		
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(usernameAlias);
		
		PrincipalAlias emailAlias = new PrincipalAlias()
			.setType(AliasType.USER_EMAIL)
			.setAlias(user.getEmail())
			.setPrincipalId(userId);
		
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(emailAlias);
		verify(notificationEmailDao).create(principalAlias);
		verify(mockPrincipalOIDCDao).bindPrincipalToSubject(userId, user.getOauthProvider(), user.getSubject());
		verifyNoMoreInteractions(mockPrincipalAliasDAO);
	}
}
