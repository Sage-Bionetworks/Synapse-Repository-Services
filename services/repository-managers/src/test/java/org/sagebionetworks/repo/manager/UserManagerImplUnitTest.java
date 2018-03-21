package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
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
	
	private final String mockId = "-1";
	private UserGroup mockUserGroup;
	private UserProfile mockUserProfile;
	private String alias;
	private PrincipalAlias principalAlias;
	
	@Before
	public void setUp() throws Exception {
		mockUserGroupDAO = mock(UserGroupDAO.class);
		mockUserProfileManger = mock(UserProfileManager.class);
		mockGroupMembersDAO = mock(GroupMembersDAO.class);
		mockAuthDAO = mock(AuthenticationDAO.class);
		basicDAO = mock(DBOBasicDao.class);
		mockPrincipalAliasDAO = mock(PrincipalAliasDAO.class);

		when(mockUserGroupDAO.create(any(UserGroup.class))).thenReturn(Long.parseLong(mockId));
		mockUserGroup = new UserGroup();
		mockUserGroup.setId(mockId);
		mockUserGroup.setIsIndividual(true);
		when(mockUserGroupDAO.get(anyLong())).thenReturn(mockUserGroup);
		
		mockUserProfile = new UserProfile();
		when(mockUserProfileManger.getUserProfile(anyString())).thenReturn(mockUserProfile);
		
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
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(alias)).thenReturn(principalAlias);
	}
	
	@Test
	public void testCreateUserAdmin() throws Exception {
		// Call with an admin
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString()+"@testing.com");
		nu.setUserName(UUID.randomUUID().toString());
		userManager.createTestUser(admin, nu, null, null);
		verify(notificationEmailDao).create((PrincipalAlias)any());
		
		// Call with a non admin
		try {
			userManager.createTestUser(notAdmin, null, null, null);
			fail();
		} catch (UnauthorizedException e) { }
		
	}
	
	@Test
	public void testDeleteUserAdmin() throws Exception {
		// Call with an admin
		userManager.deletePrincipal(admin, Long.parseLong(mockId));
		verify(mockUserGroupDAO).delete(anyString());
		
		// Call with a non admin
		try {
			userManager.deletePrincipal(notAdmin, Long.parseLong(mockId));
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
	public void testLookupUserForAuthentication() {
		// call under test
		PrincipalAlias pa = userManager.lookupUserForAuthentication(alias);
		assertEquals(principalAlias, pa);
	}
	
	@Test
	public void testLookupUserForAuthenticationTeam() {
		// set the alias as a team alias
		principalAlias.setType(AliasType.TEAM_NAME);
		try {
			// call under test
			userManager.lookupUserForAuthentication(alias);
			fail();
		} catch (UnauthenticatedException e) {
			assertEquals("Cannot authenticate as team. Only users can authenticate.",e.getMessage());
		}
	}
	
	@Test
	public void testLookupUserForAuthenticationNotFound() {
		// unknown alias
		alias = "unknown";
		try {
			// call under test
			userManager.lookupUserForAuthentication(alias);
			fail();
		} catch (NotFoundException e) {
			assertEquals("Did not find a user with alias: unknown",e.getMessage());
		}
	}
}
