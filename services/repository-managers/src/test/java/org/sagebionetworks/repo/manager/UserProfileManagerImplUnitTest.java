package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;

public class UserProfileManagerImplUnitTest {

	@Mock
	UserProfileDAO mockProfileDAO;
	@Mock
	UserGroupDAO mockUserGroupDAO;
	@Mock
	UserManager mockUserManager;
	@Mock
	FavoriteDAO mockFavoriteDAO;
	@Mock
	PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	AuthorizationManager mockAuthorizationManager;
	@Mock
	FileHandleManager mockFileHandleManager;
	@Mock
	NodeDAO mockNodeDao;
	
	UserProfileManager userProfileManager;
	
	UserInfo userInfo;
	UserInfo adminUserInfo;
	UserProfile userProfile;
	
	@Mock
	UserInfo caller;
	@Mock
	UserInfo userToGetFor;
	Team teamToFetch;
	ProjectListType type;
	ProjectListSortColumn sortColumn;
	SortDirection sortDirection;
	Long limit;
	Long offset;
	Set<Long> visibleProjectsOne;
	Set<Long> visibleProjectsTwo;
	
	private static final Long userId = 9348725L;
	private static final Long adminUserId = 823746L;
	private static final String USER_EMAIL = "foo@bar.org";
	private static final String USER_OPEN_ID = "http://myspace.com/foo";

	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		userProfileManager = new UserProfileManagerImpl();
		ReflectionTestUtils.setField(userProfileManager, "userProfileDAO", mockProfileDAO);
		ReflectionTestUtils.setField(userProfileManager, "favoriteDAO", mockFavoriteDAO);
		ReflectionTestUtils.setField(userProfileManager, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(userProfileManager, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(userProfileManager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(userProfileManager, "fileHandleManager", mockFileHandleManager);
		
		userInfo = new UserInfo(false, userId);

		adminUserInfo = new UserInfo(true, adminUserId);
		
		userProfile = new UserProfile();
		userProfile.setOwnerId(userInfo.getId().toString());
		userProfile.setRStudioUrl("myPrivateRStudioUrl");
		userProfile.setUserName("TEMPORARY-111");
		userProfile.setLocation("I'm guessing this is private");
		userProfile.setOpenIds(Collections.singletonList(USER_OPEN_ID));
		userProfile.setEmails(Collections.singletonList(USER_EMAIL));
		Settings settings = new Settings();
		settings.setSendEmailNotifications(true);
		userProfile.setNotificationSettings(settings);
		
		// UserProfileDAO should return a copy of the mock UserProfile when getting
		Mockito.doAnswer(new Answer<UserProfile>() {
			@Override
			public UserProfile answer(InvocationOnMock invocation) throws Throwable {
				DBOUserProfile intermediate = new DBOUserProfile();
				UserProfileUtils.copyDtoToDbo(userProfile, intermediate);
				UserProfile copy = UserProfileUtils.convertDboToDto(intermediate);
				return copy;
			}
		}).when(mockProfileDAO).get(Mockito.anyString());
		
		// UserProfileDAO should return a copy of the argument when updating
		Mockito.doAnswer(new Answer<UserProfile>() {
			@Override
			public UserProfile answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				UserProfile copy = (UserProfile) args[0];
				
				DBOUserProfile intermediate = new DBOUserProfile();
				UserProfileUtils.copyDtoToDbo(copy, intermediate);
				copy = UserProfileUtils.convertDboToDto(intermediate);
				return copy;
			}
		}).when(mockProfileDAO).update((UserProfile) Mockito.any());
		
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(USER_EMAIL);
		alias.setPrincipalId(userId);
		alias.setType(AliasType.USER_EMAIL);
		List<PrincipalAlias> aliases =  new ArrayList<PrincipalAlias>();
		aliases.add(alias);
		alias = new PrincipalAlias();
		alias.setAlias(USER_OPEN_ID);
		alias.setPrincipalId(userId);
		alias.setType(AliasType.USER_OPEN_ID);
		aliases.add(alias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(userId)).thenReturn(aliases);
		when(mockPrincipalAliasDAO.listPrincipalAliases(Collections.singleton(userId))).thenReturn(aliases);
		
		when(caller.getId()).thenReturn(123L);
		when(caller.isAdmin()).thenReturn(false);
		Set<Long> callersGroups = Sets.newHashSet(1L, 2L, 3L, caller.getId(),
				BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		when(caller.getGroups()).thenReturn(callersGroups);

		when(userToGetFor.getId()).thenReturn(456L);
		when(userToGetFor.isAdmin()).thenReturn(false);
		Set<Long> userToGetForGroups  = Sets.newHashSet(4L, 5L, 6L,
				userToGetFor.getId(),
				BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		teamToFetch = null;
		type = ProjectListType.MY_CREATED_PROJECTS;
		sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		sortDirection = SortDirection.ASC;
		limit = 10L;
		offset = 0L;
		
		visibleProjectsOne = Sets.newHashSet(111L,222L,333L);
		visibleProjectsTwo = Sets.newHashSet(222L,333L,444L);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);
		
	}
	
	@Test
	public void testUpdateProfileFileHandleAuthrorized() throws NotFoundException{
		String fileHandleId = "123";
		when(mockAuthorizationManager.canAccessRawFileHandleById(userInfo, fileHandleId)).thenReturn(AuthorizationStatus.authorized());
		UserProfile profile = new UserProfile();
		profile.setOwnerId(""+userInfo.getId());
		profile.setUserName("some username");
		profile.setProfilePicureFileHandleId(fileHandleId);
		userProfileManager.updateUserProfile(userInfo, profile);
		verify(mockProfileDAO).update(any(UserProfile.class));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUpdateProfileFileHandleUnAuthrorized() throws NotFoundException{
		String fileHandleId = "123";
		when(mockAuthorizationManager.canAccessRawFileHandleById(userInfo, fileHandleId)).thenReturn(AuthorizationStatus.accessDenied("User does not own the file handle"));
		UserProfile profile = new UserProfile();
		profile.setOwnerId(""+userInfo.getId());
		profile.setUserName("some username");
		profile.setProfilePicureFileHandleId(fileHandleId);
		userProfileManager.updateUserProfile(userInfo, profile);
	}
	
	@Test
	public void testAddFavorite() throws Exception {
		String entityId = "syn123";
		userProfileManager.addFavorite(userInfo, entityId);
		Favorite fav = new Favorite();
		fav.setPrincipalId(userInfo.getId().toString());
		fav.setEntityId(entityId);
		verify(mockFavoriteDAO).add(fav);
	}
	
	
	/* Tests moved and mocked from UserProfileManagerImplTest */
	
	@Test
	public void testGetOwnUserProfile() throws Exception {
		String ownerId = userInfo.getId().toString();
		UserProfile upClone = userProfileManager.getUserProfile(ownerId);
		assertEquals(userProfile, upClone);
	}
		
	@Test
	public void getAll() throws Exception {
		UserProfile upForList = new UserProfile();
		upForList.setOwnerId(userProfile.getOwnerId());
		upForList.setRStudioUrl(userProfile.getRStudioUrl());
		upForList.setUserName(userProfile.getUserName());
		upForList.setLocation(userProfile.getLocation());
		upForList.setEmails(userProfile.getEmails());
		upForList.setOpenIds(userProfile.getOpenIds());
		
		List<UserProfile> upList = Collections.singletonList(upForList);
		when(mockProfileDAO.getInRange(0L, 1L)).thenReturn(upList);
		when(mockProfileDAO.getCount()).thenReturn(1L);
		when(mockProfileDAO.list(Collections.singletonList(Long.parseLong(userProfile.getOwnerId())))).
			thenReturn(upList);

		List<UserProfile> results=userProfileManager.getInRange(adminUserInfo, 0, 1);
		assertFalse(upForList.getEmails().isEmpty());
		assertFalse(upForList.getOpenIds().isEmpty());
		assertEquals(upList, results);
		
		IdList ids = new IdList();
		ids.setList(Collections.singletonList(Long.parseLong(userProfile.getOwnerId())));
		assertEquals(results, userProfileManager.list(ids).getList());
	}
		
	@Test
	public void testGetOthersUserProfileByAdmin() throws Exception {
		String ownerId = userInfo.getId().toString();
		UserProfile upClone = userProfileManager.getUserProfile(ownerId);
		assertEquals(userProfile, upClone);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUpdateOthersUserProfile() throws Exception {
		String ownerId = userInfo.getId().toString();
		userInfo.setId(-100L);
		
		UserProfile upClone = userProfileManager.getUserProfile(ownerId);
		// so we get back the UserProfile for the specified owner...
		assertEquals(ownerId, upClone.getOwnerId());
		// ... but we can't update it, since we are not the owner or an admin
		// the following step will fail
		userProfileManager.updateUserProfile(userInfo, upClone);
	}
	
	@Test
	public void testGetGroupsMinusPublic(){
		assertTrue(caller.getGroups().contains(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()));
		assertTrue(caller.getGroups().contains(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId()));
		assertTrue(caller.getGroups().contains(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		Set<Long> results = UserProfileManagerImpl.getGroupsMinusPublic(caller.getGroups());
		// should get a new copy
		assertFalse(results == caller.getGroups());
		assertEquals(caller.getGroups().size()-3, results.size());
		// the following groups should have been removed.
		assertFalse(results.contains(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()));
		assertFalse(results.contains(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId()));
		assertFalse(results.contains(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		// The user's id should still be in the set
		assertTrue(results.contains(caller.getId()));
	}
	
	@Test
	public void testGetGroupsMinusPublicAndSelf(){
		assertTrue(caller.getGroups().contains(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()));
		assertTrue(caller.getGroups().contains(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId()));
		assertTrue(caller.getGroups().contains(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		Set<Long> results = UserProfileManagerImpl.getGroupsMinusPublicAndSelf(caller.getGroups(), caller.getId());
		// should get a new copy
		assertFalse(results == caller.getGroups());
		assertEquals(caller.getGroups().size()-4, results.size());
		// the following groups should have been removed.
		assertFalse(results.contains(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()));
		assertFalse(results.contains(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId()));
		assertFalse(results.contains(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		// The user's id should also be removed
		assertFalse(results.contains(caller.getId()));
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.
	 */
	@Test
	public void testGetProjectsNonAdminCallerDifferent(){
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertEquals(0L, results.getTotalNumberOfResults());
		// Accessible projects should be called once for the userToGetFor and once for the caller.
		verify(mockAuthorizationManager, times(2)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		verify(mockAuthorizationManager).getAccessibleProjectIds(caller.getGroups());
		// The projectIds passed to the dao should be the intersection of the caller's projects
		// and the userToGetFor's projects.
		Set<Long> expectedProjectIds = Sets.intersection(visibleProjectsOne, visibleProjectsTwo);
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are the same.
	 */
	@Test
	public void testGetProjectsNonAdminCallerSame(){
		// the caller is the same as the userToGetFor.
		caller = userToGetFor;
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		// Accessible projects should only be called once for the userToGetFor.
		verify(mockAuthorizationManager, times(1)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		// The projectIds passed to the dao should be the same as  userToGetFor can see.
		Set<Long> expectedProjectIds = visibleProjectsOne;
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	/**
	 * For this case the caller is an admin, and the caller
	 * and the userToGetFor are different.
	 */
	@Test
	public void testGetProjectsAdminCallerDifferent(){
		when(caller.isAdmin()).thenReturn(true);
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		// Accessible projects should only be called once the userToGetFor
		verify(mockAuthorizationManager, times(1)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		// The projectIds passed to the dao should be the same as  userToGetFor can see.
		Set<Long> expectedProjectIds = visibleProjectsOne;
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	/**
	 * For this case the caller is an admin, and the caller
	 * and the userToGetFor are different.
	 */
	@Test
	public void testGetProjectsAdminCallerSame(){
		caller = userToGetFor;
		when(caller.isAdmin()).thenReturn(true);
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		// Accessible projects should only be called once the userToGetFor
		verify(mockAuthorizationManager, times(1)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		// The projectIds passed to the dao should be the same as  userToGetFor can see.
		Set<Long> expectedProjectIds = visibleProjectsOne;
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is MY_PROJECTS.
	 */
	@Test
	public void testGetProjectsMY_PROJECTS(){
		type = ProjectListType.MY_PROJECTS;
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		// Accessible projects should be called once for the userToGetFor and once for the caller.
		verify(mockAuthorizationManager, times(2)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		verify(mockAuthorizationManager).getAccessibleProjectIds(caller.getGroups());
		// The projectIds passed to the dao should be the intersection of the caller's projects
		// and the userToGetFor's projects.
		Set<Long> expectedProjectIds = Sets.intersection(visibleProjectsOne, visibleProjectsTwo);
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is OTHER_USER_PROJECTS.
	 */
	@Test
	public void testGetProjectsOTHER_USER_PROJECTS(){
		type = ProjectListType.OTHER_USER_PROJECTS;
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		// Accessible projects should be called once for the userToGetFor and once for the caller.
		verify(mockAuthorizationManager, times(2)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		verify(mockAuthorizationManager).getAccessibleProjectIds(caller.getGroups());
		// The projectIds passed to the dao should be the intersection of the caller's projects
		// and the userToGetFor's projects.
		Set<Long> expectedProjectIds = Sets.intersection(visibleProjectsOne, visibleProjectsTwo);
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is MY_CREATED_PROJECTS.
	 */
	@Test
	public void testGetProjectsMY_CREATED_PROJECTS(){
		type = ProjectListType.MY_CREATED_PROJECTS;
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		// Accessible projects should be called once for the userToGetFor and once for the caller.
		verify(mockAuthorizationManager, times(2)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		verify(mockAuthorizationManager).getAccessibleProjectIds(caller.getGroups());
		// The projectIds passed to the dao should be the intersection of the caller's projects
		// and the userToGetFor's projects.
		Set<Long> expectedProjectIds = Sets.intersection(visibleProjectsOne, visibleProjectsTwo);
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is MY_PARTICIPATED_PROJECTS.
	 */
	@Test
	public void testGetProjectsMY_PARTICIPATED_PROJECTS(){
		type = ProjectListType.MY_PARTICIPATED_PROJECTS;
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		// Accessible projects should be called once for the userToGetFor and once for the caller.
		verify(mockAuthorizationManager, times(2)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		verify(mockAuthorizationManager).getAccessibleProjectIds(caller.getGroups());
		// The projectIds passed to the dao should be the intersection of the caller's projects
		// and the userToGetFor's projects.
		Set<Long> expectedProjectIds = Sets.intersection(visibleProjectsOne, visibleProjectsTwo);
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is MY_TEAM_PROJECTS.
	 */
	@Test
	public void testGetProjectsMY_TEAM_PROJECTS(){
		type = ProjectListType.MY_TEAM_PROJECTS;
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		// Accessible projects should be called once for the userToGetFor and once for the caller.
		verify(mockAuthorizationManager, times(2)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public, and the user
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublicAndSelf(userToGetFor.getGroups(), userToGetFor.getId());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		verify(mockAuthorizationManager).getAccessibleProjectIds(caller.getGroups());
		// The projectIds passed to the dao should be the intersection of the caller's projects
		// and the userToGetFor's projects.
		Set<Long> expectedProjectIds = Sets.intersection(visibleProjectsOne, visibleProjectsTwo);
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is TEAM_PROJECTS.
	 */
	@Test 
	public void testGetProjectsTEAM_PROJECTS(){
		Long teamId = 999L;
		teamToFetch = new Team();
		teamToFetch.setId(teamId.toString());
		type = ProjectListType.TEAM_PROJECTS;
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		// Accessible projects should be called once for the userToGetFor and once for the caller.
		verify(mockAuthorizationManager, times(2)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public, and the user
		Set<Long> expectedUserToGetGroups = Sets.newHashSet(teamId);
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		verify(mockAuthorizationManager).getAccessibleProjectIds(caller.getGroups());
		// The projectIds passed to the dao should be the intersection of the caller's projects
		// and the userToGetFor's projects.
		Set<Long> expectedProjectIds = Sets.intersection(visibleProjectsOne, visibleProjectsTwo);
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, limit, offset);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetProjectsTEAM_PROJECTSNullTeam(){
		teamToFetch = null;
		type = ProjectListType.TEAM_PROJECTS;
		// call under test
		PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
	}
	
	/**
	 * Must be able to call getProjects() for each type.
	 */
	@Test 
	public void testGetProjectsAllTypes(){
		Long teamId = 999L;
		teamToFetch = new Team();
		teamToFetch.setId(teamId.toString());
		type = ProjectListType.TEAM_PROJECTS;
		for(ProjectListType type: ProjectListType.values()){
			PaginatedResults<ProjectHeader> results = userProfileManager.getProjects(
					caller, userToGetFor, teamToFetch, type, sortColumn, sortDirection, limit, offset);
			assertNotNull(results);
		}
	}
	
	@Test
	public void testGetUserProfileImageUrl() {
		
		String userId = userToGetFor.getId().toString();
		String fileHandleId = "123";
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandleId)
				.withAssociation(FileHandleAssociateType.UserProfileAttachment, userId);
		
		String expectedUrl = "https://testurl.org";
		
		when(mockProfileDAO.getPictureFileHandleId(userId)).thenReturn(fileHandleId);
		when(mockFileHandleManager.getRedirectURLForFileHandle(urlRequest)).thenReturn(expectedUrl);
		
		String url = userProfileManager.getUserProfileImageUrl(userInfo, userId);
		
		verify(mockProfileDAO).getPictureFileHandleId(eq(userId));
		verify(mockFileHandleManager).getRedirectURLForFileHandle(eq(urlRequest));
		
		assertEquals(expectedUrl, url);
			
	}
	
	@Test
	public void testGetUserProfileImagePreviewUrl() {
		
		String userId = userToGetFor.getId().toString();
		String fileHandleId = "123";
		String fileHandlePreviewId = "456";
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, fileHandlePreviewId)
				.withAssociation(FileHandleAssociateType.UserProfileAttachment, userId);
		
		String expectedUrl = "https://testurl.org";
		
		when(mockProfileDAO.getPictureFileHandleId(userId)).thenReturn(fileHandleId);
		when(mockFileHandleManager.getPreviewFileHandleId(fileHandleId)).thenReturn(fileHandlePreviewId);
		when(mockFileHandleManager.getRedirectURLForFileHandle(urlRequest)).thenReturn(expectedUrl);
		
		String url = userProfileManager.getUserProfileImagePreviewUrl(userInfo, userId);
		
		verify(mockProfileDAO).getPictureFileHandleId(eq(userId));
		verify(mockFileHandleManager).getPreviewFileHandleId(eq(fileHandleId));
		verify(mockFileHandleManager).getRedirectURLForFileHandle(eq(urlRequest));
		
		assertEquals(expectedUrl, url);
		
		
	}
}
