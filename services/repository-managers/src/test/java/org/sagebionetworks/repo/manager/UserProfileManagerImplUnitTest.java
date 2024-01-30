package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ProjectHeaderList;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.favorite.SortBy;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
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
	@Mock
	AuthenticationDAO mockAuthDao;
	@InjectMocks
	UserProfileManagerImpl userProfileManager;
	
	UserInfo userInfo;
	UserInfo adminUserInfo;
	UserProfile userProfile;
	UserInfo caller;
	
	@Mock
	UserInfo userToGetFor;
	
	Long teamToFetchId;
	ProjectListType type;
	ProjectListSortColumn sortColumn;
	SortDirection sortDirection;
	String nextPageToken;
	Set<Long> visibleProjectsOne;
	Set<Long> visibleProjectsTwo;
	Set<Long> callersGroups;
	Set<Long> userToGetForGroups;
	List<PrincipalAlias> aliases;
	
	private static final Long userId = 9348725L;
	private static final Long adminUserId = 823746L;
	private static final String USER_EMAIL = "foo@bar.org";
	private static final String USER_OPEN_ID = "http://myspace.com/foo";
	private static final Long LIMIT_FOR_QUERY = NextPageToken.DEFAULT_LIMIT+1;
	private static final Long OFFSET = NextPageToken.DEFAULT_OFFSET;

	@BeforeEach
	public void before() throws Exception {
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
		userProfile.setTwoFactorAuthEnabled(true);
		
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(USER_EMAIL);
		alias.setPrincipalId(userId);
		alias.setType(AliasType.USER_EMAIL);
		aliases =  new ArrayList<PrincipalAlias>();
		aliases.add(alias);
		alias = new PrincipalAlias();
		alias.setAlias(USER_OPEN_ID);
		alias.setPrincipalId(userId);
		alias.setType(AliasType.USER_OPEN_ID);
		aliases.add(alias);
		caller = new UserInfo(false, 123L);
		callersGroups = Sets.newHashSet(1L, 2L, 3L, caller.getId(),
				BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		caller.setGroups(callersGroups);
		userToGetForGroups = Sets.newHashSet(4L, 5L, 6L,
				userToGetFor.getId(),
				BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		teamToFetchId = null;
		type = ProjectListType.CREATED;
		sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		sortDirection = SortDirection.ASC;
		nextPageToken = (new NextPageToken(null)).toToken();
		
		visibleProjectsOne = Sets.newHashSet(111L,222L,333L);
		visibleProjectsTwo = Sets.newHashSet(222L,333L,444L);
	}
	
	@Test
	public void testUpdateProfileFileHandleAuthrorized() throws NotFoundException{
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
		
		String fileHandleId = "123";
		when(mockAuthorizationManager.canAccessRawFileHandleById(userInfo, fileHandleId)).thenReturn(AuthorizationStatus.authorized());
		UserProfile profile = new UserProfile();
		profile.setOwnerId(""+userInfo.getId());
		profile.setUserName("some username");
		profile.setProfilePicureFileHandleId(fileHandleId);
		userProfileManager.updateUserProfile(userInfo, profile);
		verify(mockProfileDAO).update(any(UserProfile.class));
	}
	
	@Test
	public void testUpdateProfileFileHandleUnAuthrorized() throws NotFoundException{										
		String fileHandleId = "123";
		when(mockAuthorizationManager.canAccessRawFileHandleById(userInfo, fileHandleId)).thenReturn(AuthorizationStatus.accessDenied("User does not own the file handle"));
		UserProfile profile = new UserProfile();
		profile.setOwnerId(""+userInfo.getId());
		profile.setUserName("some username");
		profile.setProfilePicureFileHandleId(fileHandleId);
		assertThrows(UnauthorizedException.class, () -> {
			userProfileManager.updateUserProfile(userInfo, profile);
		});
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
	
	@Test
	public void testAddFavoriteAsAnonymous() throws Exception {
		String entityId = "syn123";
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
		assertThrows(UnauthorizedException.class, ()->{
			userProfileManager.addFavorite(userInfo, entityId);
		});
	}
	
	@Test
	public void testRemoveFavoriteAsAnonymous() throws Exception {
		String entityId = "syn123";
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
		assertThrows(UnauthorizedException.class, ()->{
			userProfileManager.removeFavorite(userInfo, entityId);
		});
	}
	
	@Test
	public void testGetFavoriteAsAnonymous() throws Exception {
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
		assertTrue(
			userProfileManager.getFavorites(userInfo, 10, 0, SortBy.FAVORITED_ON, org.sagebionetworks.repo.model.favorite.SortDirection.ASC).getResults().isEmpty()
		);
		verify(mockFavoriteDAO, never()).getFavoritesEntityHeader(anyString(), anyInt(), anyInt(), any(SortBy.class), any(org.sagebionetworks.repo.model.favorite.SortDirection.class));
	}
	
	/* Tests moved and mocked from UserProfileManagerImplTest */
	
	@Test
	public void testGetOwnUserProfile() throws Exception {
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
		
		when(mockPrincipalAliasDAO.listPrincipalAliases(userId)).thenReturn(aliases);
		when(mockAuthDao.getTwoFactorAuthStateMap(any())).thenReturn(Map.of(userId, true));
		
		String ownerId = userInfo.getId().toString();
		UserProfile upClone = userProfileManager.getUserProfile(ownerId);
		assertEquals(userProfile, upClone);
	}
		
	@Test
	public void getAll() throws Exception {
		when(mockPrincipalAliasDAO.listPrincipalAliases(Collections.singleton(userId))).thenReturn(aliases);
		when(mockAuthDao.getTwoFactorAuthStateMap(any())).thenReturn(Map.of(userId, true));
		
		UserProfile upForList = new UserProfile();
		upForList.setOwnerId(userProfile.getOwnerId());
		upForList.setRStudioUrl(userProfile.getRStudioUrl());
		upForList.setUserName(userProfile.getUserName());
		upForList.setLocation(userProfile.getLocation());
		upForList.setEmails(userProfile.getEmails());
		upForList.setOpenIds(userProfile.getOpenIds());
		
		List<UserProfile> upList = Collections.singletonList(upForList);
		when(mockProfileDAO.getInRange(0L, 1L)).thenReturn(upList);

		List<UserProfile> results=userProfileManager.getInRange(adminUserInfo, 0, 1);
		
		assertFalse(upForList.getEmails().isEmpty());
		assertFalse(upForList.getOpenIds().isEmpty());
		
		assertEquals(upList, results);
		
		when(mockProfileDAO.list(Collections.singletonList(Long.parseLong(userProfile.getOwnerId())))).thenReturn(upList);
		
		IdList ids = new IdList();
		ids.setList(Collections.singletonList(Long.parseLong(userProfile.getOwnerId())));
		assertEquals(results, userProfileManager.list(ids).getList());
	}
		
	@Test
	public void testGetOthersUserProfileByAdmin() throws Exception {
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
		
		when(mockPrincipalAliasDAO.listPrincipalAliases(userId)).thenReturn(aliases);
		when(mockAuthDao.getTwoFactorAuthStateMap(any())).thenReturn(Map.of(userId, true));
		
		String ownerId = userInfo.getId().toString();
		UserProfile upClone = userProfileManager.getUserProfile(ownerId);
		assertEquals(userProfile, upClone);
	}
	
	@Test
	public void testUpdateOthersUserProfile() throws Exception {
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
				
		when(mockPrincipalAliasDAO.listPrincipalAliases(userId)).thenReturn(aliases);
				
		String ownerId = userInfo.getId().toString();
		userInfo.setId(-100L);
		
		UserProfile upClone = userProfileManager.getUserProfile(ownerId);
		// so we get back the UserProfile for the specified owner...
		assertEquals(ownerId, upClone.getOwnerId());
		// ... but we can't update it, since we are not the owner or an admin
		// the following step will fail
		assertThrows(UnauthorizedException.class, () -> {
			userProfileManager.updateUserProfile(userInfo, upClone);
		});
	}
	
	@Test
	public void testUpdateAnonymousUserProfile() throws Exception {
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
		
		assertThrows(UnauthorizedException.class, () -> {
			userProfileManager.updateUserProfile(userInfo, userProfile);
		});
		
		verify(mockProfileDAO, never()).update(any(UserProfile.class));
	}
	
	@Test
	public void testGetGroupsMinusPublic(){
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
		when(userToGetFor.getId()).thenReturn(456L);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
		assertNotNull(results);
		assertNotNull(results.getResults());
		assertNull(results.getNextPageToken());
		// Accessible projects should be called once for the userToGetFor and once for the caller.
		verify(mockAuthorizationManager, times(2)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		verify(mockAuthorizationManager).getAccessibleProjectIds(caller.getGroups());
		// The projectIds passed to the dao should be the intersection of the caller's projects
		// and the userToGetFor's projects.
		Set<Long> expectedProjectIds = Sets.intersection(visibleProjectsOne, visibleProjectsTwo);
		verify(mockNodeDao).getProjectHeaders(userToGetFor.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are the same.
	 */
	@Test
	public void testGetProjectsNonAdminCallerSame(){
		when(userToGetFor.getId()).thenReturn(456L);
		when(userToGetFor.isAdmin()).thenReturn(false);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		// the caller is the same as the userToGetFor.
		caller = userToGetFor;
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
		assertNotNull(results);
		// Accessible projects should only be called once for the userToGetFor.
		verify(mockAuthorizationManager, times(1)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		// The projectIds passed to the dao should be the same as  userToGetFor can see.
		Set<Long> expectedProjectIds = visibleProjectsOne;
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	/**
	 * For this case the caller is an admin, and the caller
	 * and the userToGetFor are different.
	 */
	@Test
	public void testGetProjectsAdminCallerDifferent(){						
		when(userToGetFor.getId()).thenReturn(456L);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				adminUserInfo, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
		assertNotNull(results);
		// Accessible projects should only be called once the userToGetFor
		verify(mockAuthorizationManager, times(1)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		// The projectIds passed to the dao should be the same as  userToGetFor can see.
		Set<Long> expectedProjectIds = visibleProjectsOne;
		verify(mockNodeDao).getProjectHeaders(userToGetFor.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	/**
	 * For this case the caller is an admin, and the caller
	 * and the userToGetFor are different.
	 */
	@Test
	public void testGetProjectsAdminCallerSame(){
		when(userToGetFor.getId()).thenReturn(456L);
		when(userToGetFor.isAdmin()).thenReturn(false);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		caller = userToGetFor;
		when(caller.isAdmin()).thenReturn(true);
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
		assertNotNull(results);
		// Accessible projects should only be called once the userToGetFor
		verify(mockAuthorizationManager, times(1)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public.
		Set<Long> expectedUserToGetGroups = UserProfileManagerImpl.getGroupsMinusPublic(userToGetFor.getGroups());
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		// The projectIds passed to the dao should be the same as  userToGetFor can see.
		Set<Long> expectedProjectIds = visibleProjectsOne;
		verify(mockNodeDao).getProjectHeaders(caller.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is ALL.
	 */
	@Test
	public void testGetProjectsMY_PROJECTS(){
		when(userToGetFor.getId()).thenReturn(456L);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		type = ProjectListType.ALL;
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
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
		verify(mockNodeDao).getProjectHeaders(userToGetFor.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is ALL.
	 */
	@Test
	public void testGetProjectsOTHER_USER_PROJECTS(){				
		when(userToGetFor.getId()).thenReturn(456L);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		type = ProjectListType.ALL;
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
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
		verify(mockNodeDao).getProjectHeaders(userToGetFor.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is CREATED.
	 */
	@Test
	public void testGetProjectsMY_CREATED_PROJECTS(){
		when(userToGetFor.getId()).thenReturn(456L);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		type = ProjectListType.CREATED;
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
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
		verify(mockNodeDao).getProjectHeaders(userToGetFor.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is PARTICIPATED.
	 */
	@Test
	public void testGetProjectsMY_PARTICIPATED_PROJECTS(){						
		when(userToGetFor.getId()).thenReturn(456L);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		type = ProjectListType.PARTICIPATED;
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
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
		verify(mockNodeDao).getProjectHeaders(userToGetFor.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is TEAM and
	 * we specify one particular tem.
	 */
	@Test
	public void testGetProjectsMY_TEAM_PROJECTS(){
		when(userToGetFor.getId()).thenReturn(456L);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		teamToFetchId = null;
		type = ProjectListType.TEAM;
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
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
		verify(mockNodeDao).getProjectHeaders(userToGetFor.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	/**
	 * For this case the caller is not an admin, and the caller
	 * and the userToGetFor are different.  The type is TEAM and
	 * we allow all teams.
	 */
	@Test 
	public void testGetProjectsTEAM_PROJECTS(){
		when(userToGetFor.getId()).thenReturn(456L);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		teamToFetchId = 999L;
		userToGetFor.getGroups().add(teamToFetchId);
		type = ProjectListType.TEAM;
		// call under test
		ProjectHeaderList results = userProfileManager.getProjects(
				caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
		assertNotNull(results);
		// Accessible projects should be called once for the userToGetFor and once for the caller.
		verify(mockAuthorizationManager, times(2)).getAccessibleProjectIds(anySetOf(Long.class));
		// the groups for the userToGetFor should exclude public, and the user
		Set<Long> expectedUserToGetGroups = Sets.newHashSet(teamToFetchId);
		verify(mockAuthorizationManager).getAccessibleProjectIds(expectedUserToGetGroups);
		verify(mockAuthorizationManager).getAccessibleProjectIds(caller.getGroups());
		// The projectIds passed to the dao should be the intersection of the caller's projects
		// and the userToGetFor's projects.
		Set<Long> expectedProjectIds = Sets.intersection(visibleProjectsOne, visibleProjectsTwo);
		verify(mockNodeDao).getProjectHeaders(userToGetFor.getId(), expectedProjectIds, type, sortColumn, sortDirection, LIMIT_FOR_QUERY, OFFSET);
	}
	
	@Test
	public void testGetProjectsNonTeamWithTeamId() {		
		teamToFetchId = 999L;
		type = ProjectListType.ALL;
		try {
			userProfileManager.getProjects(
					caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
	}
	
	/**
	 * Must be able to call getProjects() for each type.
	 */
	@Test 
	public void testGetProjectsAllTypes(){
		when(userToGetFor.getId()).thenReturn(456L);

		when(userToGetFor.getGroups()).thenReturn(userToGetForGroups);
		
		when(mockAuthorizationManager.getAccessibleProjectIds(anySetOf(Long.class))).thenReturn(
				visibleProjectsOne,
				visibleProjectsTwo
				);		
		
		userToGetFor.getGroups().add(teamToFetchId);
		for(ProjectListType type: ProjectListType.values()) {
			ProjectHeaderList results = userProfileManager.getProjects(
					caller, userToGetFor, teamToFetchId, type, sortColumn, sortDirection, nextPageToken);
			assertNotNull(results);
		}
	}
	
	@Test
	public void testGetUserProfileImageUrl() {
		when(userToGetFor.getId()).thenReturn(456L);
		
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
		when(userToGetFor.getId()).thenReturn(456L);
		
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
