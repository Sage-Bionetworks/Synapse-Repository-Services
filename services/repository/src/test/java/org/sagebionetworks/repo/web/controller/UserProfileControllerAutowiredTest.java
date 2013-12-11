package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.repo.web.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserProfileControllerAutowiredTest {
	
	@Autowired
	private UserProfileService userProfileService;
	
	@Autowired
	private ServletTestHelper testHelper;
	
	@Autowired
	private UserManager userManager;

	@Autowired 
	private EntityService entityService;

	private static HttpServlet dispatchServlet;
	
	private String username;
	private UserInfo testUser;

	private List<String> favoritesToDelete;
	private List<String> entityIdsToDelete;

	HttpServletRequest mockRequest;

	@BeforeClass
	public static void beforeClass() throws ServletException {
		dispatchServlet = DispatchServletSingleton.getInstance();
	}

	@Before
	public void before() throws Exception{
		username = userManager.getGroupName(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		
		testHelper.setUp();
		assertNotNull(userProfileService);
		favoritesToDelete = new ArrayList<String>();
		entityIdsToDelete = new ArrayList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(username);
		UserInfo.validateUserInfo(testUser);
		
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
	}
	
	@After
	public void after() throws UnauthorizedException {
		if (userProfileService != null && favoritesToDelete != null) {
			for (String entityId : favoritesToDelete) {
				try {
					userProfileService.removeFavorite(username, entityId);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
		if (entityService != null && entityIdsToDelete != null) {
			for (String idToDelete : entityIdsToDelete) {
				try {
					entityService.deleteEntity(username, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}		
	}
	
	
	@Test
	public void testGetUserGroupHeadersNoFilter() throws Exception {
		String prefix = "";
		int limit = 15;
		int offset = 0;
		
		@SuppressWarnings("static-access")
		UserGroupHeaderResponsePage response = testHelper.getUserGroupHeadersByPrefix(prefix, limit, offset);
		assertNotNull(response);
		List<UserGroupHeader> children = response.getChildren();
		assertNotNull(children);
		
		// Should find AUTHENTICATED_USERS group in results.
		Set<String> names = new HashSet<String>();		
		for (UserGroupHeader ugh : children) {
			names.add(ugh.getDisplayName());
		}
		assertTrue("Expected 'AUTHENTICATED_USERS' group, but was not found.", names.contains("AUTHENTICATED_USERS"));
	}
	
	
	
	@Test
	public void testGeUserGroupHeadersWithFilter() throws Exception {
		String prefix = "dev";
		int limit = 10;
		int offset = 0;
		
		@SuppressWarnings("static-access")
		UserGroupHeaderResponsePage response = testHelper.getUserGroupHeadersByPrefix(prefix, limit, offset);
		assertNotNull(response);
		List<UserGroupHeader> children = response.getChildren();
		assertNotNull(children);
		
		// Verify prefix filtering.
		for (UserGroupHeader ugh : children) {
			if (!ugh.getDisplayName().toLowerCase().startsWith(prefix.toLowerCase()))
				fail("Invalid user/group returned: '" + ugh.getDisplayName() + "' does not match prefix '" + prefix +"'.");
		}
		
	}
		
	@Test
	public void testFavoriteCRUD() throws Exception {
		// create an entity
		Project proj = new Project();
		proj.setEntityType(Project.class.getName());
		proj = entityService.createEntity(username, proj, null, mockRequest);
		entityIdsToDelete.add(proj.getId());
		
		// add favorite
		Map<String, String> extraParams = new HashMap<String, String>();
		EntityHeader fav = ServletTestHelper.addFavorite(dispatchServlet, proj.getId(), username, extraParams);
		favoritesToDelete.add(fav.getId());
		assertNotNull(fav);

		// retrieve
		extraParams = new HashMap<String, String>();
		extraParams.put("offset", "0");
		extraParams.put("limit", Integer.toString(Integer.MAX_VALUE));
		PaginatedResults<EntityHeader> favs = ServletTestHelper.getFavorites(dispatchServlet, username, extraParams);
		assertNotNull(favs);
		assertEquals(1, favs.getTotalNumberOfResults());
		assertEquals(1, favs.getResults().size());
		assertEquals(fav.getId(), favs.getResults().get(0).getId());
		
		// test removal
		extraParams = new HashMap<String, String>();
		ServletTestHelper.removeFavorite(dispatchServlet, proj.getId(), username, extraParams);
		// assure deletion
		extraParams = new HashMap<String, String>();
		extraParams.put("offset", "0");
		extraParams.put("limit", Integer.toString(Integer.MAX_VALUE));
		favs = ServletTestHelper.getFavorites(dispatchServlet, username, extraParams);
		assertEquals(0, favs.getTotalNumberOfResults());
		assertEquals(0, favs.getResults().size());
	}
	
}
