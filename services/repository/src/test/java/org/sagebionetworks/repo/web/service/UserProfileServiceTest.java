package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserProfileServiceTest {
	
	@Autowired
	UserProfileService userProfileService;
	
	private PermissionsManager mockPermissionsManager;
	private UserProfileManager mockUserProfileManager;
	
	@Before
	public void before() throws Exception {
		mockPermissionsManager = mock(PermissionsManager.class);
		mockUserProfileManager = mock(UserProfileManager.class);
		
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
		
		when(mockPermissionsManager.getGroups()).thenReturn(groups);
		when(mockUserProfileManager.getInRange(any(UserInfo.class), anyLong(), anyLong())).thenReturn(profiles);
		when(mockUserProfileManager.getInRange(any(UserInfo.class), anyLong(), anyLong(), eq(true))).thenReturn(profiles);
		
		userProfileService.setPermissionsManager(mockPermissionsManager);
		userProfileService.setUserProfileManager(mockUserProfileManager);
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
	public void testGeUserGroupHeadersWithSameCaseFilter() throws ServletException, IOException, DatastoreException, NotFoundException {
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
	public void testGeUserGroupHeadersWithDifferentCaseFilter() throws ServletException, IOException, DatastoreException, NotFoundException {
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
	public void testGeUserGroupHeadersWithFilterSameName() throws ServletException, IOException, DatastoreException, NotFoundException {
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
	
}
