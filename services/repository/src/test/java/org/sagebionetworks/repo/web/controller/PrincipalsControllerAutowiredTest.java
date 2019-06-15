package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a an integration test for the PrincipalsController.
 * 
 * @author jmhill, adapted by bhoff
 * 
 */
public class PrincipalsControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	// Used for cleanup
	@Autowired
	private EntityService entityController;
	
	@Autowired
	public UserManager userManager;

	private Long adminUserId;
	private UserInfo testUser;

	private List<String> toDelete;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(adminUserId);
		UserInfo.validateUserInfo(testUser);
	}

	@After
	public void after() throws UnauthorizedException {
		if (entityController != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					entityController.deleteEntity(adminUserId, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}

	@Test
	public void testGetUsers() throws Exception {
		PaginatedResults<UserProfile> userProfiles = servletTestHelper.getUsers(dispatchServlet, adminUserId);
		assertNotNull(userProfiles);
		for (UserProfile userProfile : userProfiles.getResults()) {
			System.out.println(userProfile);
		}
	}
	
	@Test
	public void testGetUsersAnonymouslyShouldFail() throws ServletException, IOException{
		try {
			servletTestHelper.getUsers(dispatchServlet, null);
			fail("Exception expected.");
		} catch (Exception e) {
			// as expected
		}
		
	}
	
	@Test
	public void testGetGroups() throws Exception {
		PaginatedResults<UserGroup> ugs = servletTestHelper.getGroups(dispatchServlet, adminUserId);
		assertNotNull(ugs);
		boolean foundPublic = false;
		boolean foundAdmin = false;
		for (UserGroup ug : ugs.getResults()) {
			if (ug.getId().equals(
					BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()
							.toString())) {
				foundPublic = true;
			}
			if (ug.getId().equals(
					TeamConstants.ADMINISTRATORS_TEAM_ID.toString())) {
				foundAdmin = true;
			}
			assertTrue(ug.toString(), !ug.getIsIndividual());
		}
		assertTrue(foundPublic);
		assertTrue(foundAdmin);
	}
	
	@Test
	public void testGetGroupsAnonymouslyShouldFail() throws ServletException, IOException{
		try {
			servletTestHelper.getGroups(dispatchServlet, null);
			fail("Exception expected.");
		} catch (Exception e) {
			// as expected
		}
		
	}
	

}
