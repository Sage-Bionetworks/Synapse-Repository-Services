package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.NotFoundException;

public class UserProfileServiceTest {
	
	private static final String EXTRA_USER_ID = "foo";
	private static UserProfile extraProfile;
	private static UserInfo userInfo;
	
	private UserProfileService userProfileService = new UserProfileServiceImpl();
	
	private EntityPermissionsManager mockPermissionsManager;
	private UserProfileManager mockUserProfileManager;
	private UserManager mockUserManager;
	private EntityManager mockEntityManager;
	
	@Before
	public void before() throws Exception {
		mockPermissionsManager = mock(EntityPermissionsManager.class);
		mockUserProfileManager = mock(UserProfileManager.class);
		mockUserManager = mock(UserManager.class);
		mockEntityManager = mock(EntityManager.class);
		
		
		// Create UserGroups
		Collection<UserGroup> groups = new HashSet<UserGroup>();
		for (int i = 0; i < 10; i++) {
			UserGroup g = new UserGroup();
			g.setId("g" + i);
			g.setIsIndividual(false);
			g.setName("Group " + i);
			groups.add(g);
		}
		
		// Create UserProfiles
		List<UserProfile> list = new ArrayList<UserProfile>();
		for (int i = 0; i < 10; i++) {
			UserProfile p = new UserProfile();
			p.setOwnerId("p" + i);
			p.setDisplayName("User " + i);
			list.add(p);
		}
		// extra profile with duplicated name
		UserProfile p = new UserProfile();
		p.setOwnerId("p0_duplicate");
		p.setDisplayName("User 0");
		list.add(p);
		QueryResults<UserProfile> profiles = new QueryResults<UserProfile>(list, list.size());
		
		extraProfile = new UserProfile();
		extraProfile.setOwnerId(EXTRA_USER_ID);
		extraProfile.setDisplayName("This UserProfile was created after the cache was last refreshed.");
		userInfo = new UserInfo(false);
		userInfo.setIndividualGroup(new UserGroup());
		userInfo.getIndividualGroup().setId(EXTRA_USER_ID);

		when(mockUserProfileManager.getInRange(any(UserInfo.class), anyLong(), anyLong())).thenReturn(profiles);
		when(mockUserProfileManager.getInRange(any(UserInfo.class), anyLong(), anyLong(), eq(true))).thenReturn(profiles);
		when(mockUserProfileManager.getUserProfile(any(UserInfo.class), eq(EXTRA_USER_ID))).thenReturn(extraProfile);
		when(mockUserManager.getUserInfo(EXTRA_USER_ID)).thenReturn(userInfo);
		when(mockUserManager.getGroups()).thenReturn(groups);

		userProfileService.setPermissionsManager(mockPermissionsManager);
		userProfileService.setUserProfileManager(mockUserProfileManager);
		userProfileService.setUserManager(mockUserManager);
		userProfileService.setEntityManager(mockEntityManager);
	}
	
	@Test
	public void testGetUserGroupHeadersById() throws DatastoreException, NotFoundException {
		List<String> ids = new ArrayList<String>();
		ids.add("g0");
		ids.add("g1");
		ids.add("g2");
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByIds(null, ids);
		Map<String, UserGroupHeader> headers = new HashMap<String, UserGroupHeader>();
		for (UserGroupHeader ugh : response.getChildren())
			headers.put(ugh.getOwnerId(), ugh);
		assertEquals(3, headers.size());
		assertTrue(headers.containsKey("g0"));
		assertTrue(headers.containsKey("g1"));
		assertTrue(headers.containsKey("g2"));
	}
	
	@Test
	public void testGetUserGroupHeadersByIdNotInCache() throws DatastoreException, NotFoundException {
		List<String> ids = new ArrayList<String>();
		ids.add("g0");
		ids.add("g1");
		ids.add("g2");
		ids.add(EXTRA_USER_ID); // should require fetch from repo
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByIds(null, ids);
		Map<String, UserGroupHeader> headers = new HashMap<String, UserGroupHeader>();
		for (UserGroupHeader ugh : response.getChildren())
			headers.put(ugh.getOwnerId(), ugh);
		assertEquals(4, headers.size());
		assertTrue(headers.containsKey("g0"));
		assertTrue(headers.containsKey("g1"));
		assertTrue(headers.containsKey("g2"));
		assertTrue(headers.containsKey(EXTRA_USER_ID));
		
		verify(mockUserProfileManager).getUserProfile(any(UserInfo.class), eq(EXTRA_USER_ID));
	}
	
	@Test(expected = NotFoundException.class)
	public void testGetUserGroupHeadersByIdDoesNotExist() throws DatastoreException, NotFoundException {
		List<String> ids = new ArrayList<String>();
		ids.add("g0");
		ids.add("g1");
		ids.add("g2");
		ids.add("g10"); // should not exist
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByIds(null, ids);
		Map<String, UserGroupHeader> headers = new HashMap<String, UserGroupHeader>();
		for (UserGroupHeader ugh : response.getChildren())
			headers.put(ugh.getOwnerId(), ugh);
		assertEquals(3, headers.size());
		assertTrue(headers.containsKey("g0"));
		assertTrue(headers.containsKey("g1"));
		assertTrue(headers.containsKey("g2"));
		assertFalse(headers.containsKey("g10"));
	}
	
	@Test
	public void testGetUserGroupHeadersNoFilter() throws ServletException, IOException, DatastoreException, NotFoundException {
		String prefix = "";
		int limit = 15;
		int offset = 0;
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByPrefix(prefix, offset, limit, null, null);
		assertNotNull(response);
		List<UserGroupHeader> children = response.getChildren();
		assertNotNull(children);
		
		assertEquals("Incorrect number of results", children.size(), limit);

		Set<String> names = new HashSet<String>();		
		for (UserGroupHeader ugh : children) {
			names.add(ugh.getDisplayName());
		}
		// spot check: should find first 15 alphabetical names
		assertTrue("Expected 'Group 5', but was not found.", names.contains("Group 5"));
		assertFalse("Did not expect 'User 5', but was found.", names.contains("User 5"));
	}
	
	
	
	@Test
	public void testGetUserGroupHeadersWithSameCaseFilter() throws ServletException, IOException, DatastoreException, NotFoundException {
		String prefix = "Gro";
		int limit = Integer.MAX_VALUE;
		int offset = 0;
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByPrefix(prefix, offset, limit, null, null);
		assertNotNull(response);
		List<UserGroupHeader> children = response.getChildren();
		assertNotNull(children);
		
		assertEquals("Incorrect number of results", 10, children.size());

		Set<String> names = new HashSet<String>();		
		for (UserGroupHeader ugh : children) {
			names.add(ugh.getDisplayName());
		}
		// check: should find all 10 UserGroups and no UserProfiles
		for (int i = 0; i < 10; i++) {
			assertTrue("Expected 'Group " + i + "', but was not found.", names.contains("Group " + i));
			assertFalse("Did not expect 'User " + i + "', but was found.", names.contains("User " + i));	
		}
	}
	
	@Test
	public void testGetUserGroupHeadersWithDifferentCaseFilter() throws ServletException, IOException, DatastoreException, NotFoundException {
		String prefix = "gRoUp";
		int limit = Integer.MAX_VALUE;
		int offset = 0;
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByPrefix(prefix, offset, limit, null, null);
		assertNotNull(response);
		List<UserGroupHeader> children = response.getChildren();
		assertNotNull(children);
		
		assertEquals("Incorrect number of results", 10, children.size());

		Set<String> names = new HashSet<String>();		
		for (UserGroupHeader ugh : children) {
			names.add(ugh.getDisplayName());
		}
		// check: should find all 10 UserGroups and no UserProfiles
		for (int i = 0; i < 10; i++) {
			assertTrue("Expected 'Group " + i + "', but was not found.", names.contains("Group " + i));
			assertFalse("Did not expect 'User " + i + "', but was found.", names.contains("User " + i));	
		}
	}
	
	@Test
	public void testGetUserGroupHeadersWithFilterSameName() throws ServletException, IOException, DatastoreException, NotFoundException {
		String prefix = "user 0";
		int limit = Integer.MAX_VALUE;
		int offset = 0;
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByPrefix(prefix, offset, limit, null, null);
		assertNotNull(response);
		List<UserGroupHeader> children = response.getChildren();
		assertNotNull(children);
		
		assertEquals("Expected different number of results", 2, children.size());

		Set<String> ids = new HashSet<String>();
		for (UserGroupHeader ugh : children) {
			assertEquals("Invalid header returned", "User 0", ugh.getDisplayName());
			ids.add(ugh.getOwnerId());
		}
		assertTrue("Expected principal was not returned", ids.contains("p0"));
		assertTrue("Expected principal was not returned", ids.contains("p0_duplicate"));
	}
	
	@Test
	public void testGetUserGroupHeadersWithFilterByLastName() throws ServletException, IOException, DatastoreException, NotFoundException {
		String prefix = "0";
		int limit = Integer.MAX_VALUE;
		int offset = 0;
		
		UserGroupHeaderResponsePage response = userProfileService.getUserGroupHeadersByPrefix(prefix, offset, limit, null, null);
		assertNotNull(response);
		List<UserGroupHeader> children = response.getChildren();
		assertNotNull(children);
		
		assertEquals("Expected different number of results", 3, children.size());

		Set<String> ids = new HashSet<String>();
		for (UserGroupHeader ugh : children) {
			ids.add(ugh.getOwnerId());
		}
		assertTrue("Expected profile 0, but was not found", ids.contains("p0"));
		assertTrue("Expected profile 0 duplicate, but was not found", ids.contains("p0_duplicate"));
		assertTrue("Expected group 0, but was not found.", ids.contains("g0"));
	}


	@Test
	public void testAddFavorite() throws Exception {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo)).thenReturn(true);		
		Favorite fav = new Favorite();
		fav.setEntityId(entityId);
		fav.setPrincipalId(EXTRA_USER_ID);
		when(mockUserProfileManager.addFavorite(any(UserInfo.class), anyString())).thenReturn(fav);

		userProfileService.addFavorite(EXTRA_USER_ID, entityId);
		
		verify(mockUserProfileManager).addFavorite(userInfo, entityId);
		verify(mockEntityManager).getEntityHeader(userInfo, entityId, null);
	}

	@Test(expected=UnauthorizedException.class)
	public void testAddFavoriteUnauthorized() throws Exception {
		String entityId = "syn123";
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo)).thenReturn(false);		
		Favorite fav = new Favorite();
		fav.setEntityId(entityId);
		fav.setPrincipalId(EXTRA_USER_ID);
		when(mockUserProfileManager.addFavorite(any(UserInfo.class), anyString())).thenReturn(fav);

		userProfileService.addFavorite(EXTRA_USER_ID, entityId);		
		fail();
	}
	
	@Test
	public void testPrivateFieldCleaning() throws Exception {
		String profileId = "someOtherProfileid";
		String ownerId = "ownerId";
		String email = "test@example.com";
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(ownerId);
		userProfile.setEmail(email);
		when(mockUserManager.getUserInfo(EXTRA_USER_ID)).thenReturn(userInfo);
		when(mockUserProfileManager.getUserProfile(userInfo, profileId)).thenReturn(userProfile);
		
		UserProfile someOtherUserProfile = userProfileService.getUserProfileByOwnerId(EXTRA_USER_ID, profileId);
		assertFalse(email.equals(someOtherUserProfile.getEmail()));
		assertNull(someOtherUserProfile.getEtag());
	}

	@Test
	public void testPrivateFieldCleaningAdmin() throws Exception {
		String profileId = "someOtherProfileid";
		String ownerId = "ownerId";
		String email = "test@example.com";
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(ownerId);
		userProfile.setEmail(email);

		userInfo = new UserInfo(true);
		userInfo.setIndividualGroup(new UserGroup());
		userInfo.getIndividualGroup().setId(EXTRA_USER_ID);
		when(mockUserManager.getUserInfo(EXTRA_USER_ID)).thenReturn(userInfo);
		when(mockUserProfileManager.getUserProfile(userInfo, profileId)).thenReturn(userProfile);
		
		UserProfile someOtherUserProfile = userProfileService.getUserProfileByOwnerId(EXTRA_USER_ID, profileId);
		assertEquals(email, someOtherUserProfile.getEmail());
		assertNull(someOtherUserProfile.getEtag());
	}


}
