package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * This is a an integration test for the PrincipalsController.
 * 
 * @author jmhill, adapted by bhoff
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PrincipalsControllerAutowiredTest {

	// Used for cleanup
	@Autowired
	EntityService entityController;
	
	@Autowired
	public UserManager userManager;

	static private Log log = LogFactory
			.getLog(PrincipalsControllerAutowiredTest.class);

	private static HttpServlet dispatchServlet;
	
	private String userName = TestUserDAO.ADMIN_USER_NAME;
	private UserInfo testUser;

	private List<String> toDelete;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userName);
		UserInfo.validateUserInfo(testUser);
	}

	@After
	public void after() throws UnauthorizedException {
		if (entityController != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					entityController.deleteEntity(userName, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}

	@BeforeClass
	public static void beforeClass() throws ServletException {
		// Setup the servlet once
		// Create a Spring MVC DispatcherServlet so that we can test our URL
		// mapping, request format, response format, and response status
		// code.
		MockServletConfig servletConfig = new MockServletConfig("repository");
		servletConfig.addInitParameter("contextConfigLocation",
				"classpath:test-context.xml");
		dispatchServlet = new DispatcherServlet();
		dispatchServlet.init(servletConfig);

	}


	@Test
	public void testGetUsers() throws ServletException, IOException{
		PaginatedResults<UserProfile> userProfiles = ServletTestHelper.getUsers(dispatchServlet, userName);
		assertNotNull(userProfiles);
		boolean foundAnon = false;
		for (UserProfile userProfile : userProfiles.getResults()) {
			System.out.println(userProfile);
//			if (userProfile.getDisplayName().equals(AuthorizationConstants.ANONYMOUS_USER_ID)) foundAnon=true;
		}
//		assertTrue(foundAnon);
	}
	
	@Test
	public void testGetUsersAnonymouslyShouldFail() throws ServletException, IOException{
		try {
			ServletTestHelper.getUsers(dispatchServlet, null);
			fail("Exception expected.");
		} catch (Exception e) {
			// as expected
		}
		
	}
	
	@Test
	public void testGetGroups() throws ServletException, IOException{
		PaginatedResults<UserGroup> ugs = ServletTestHelper.getGroups(dispatchServlet, userName);
		assertNotNull(ugs);
		boolean foundPublic = false;
		boolean foundAdmin = false;
		for (UserGroup ug : ugs.getResults()) {
			if (ug.getName().equals(AuthorizationConstants.PUBLIC_GROUP_NAME)) foundPublic=true;
			if (ug.getName().equals(AuthorizationConstants.ADMIN_GROUP_NAME)) foundAdmin=true;
			assertTrue(ug.toString(), !ug.getIsIndividual());
		}
		assertTrue(foundPublic);
	}
	
	@Test
	public void testGetGroupsAnonymouslyShouldFail() throws ServletException, IOException{
		try {
			ServletTestHelper.getGroups(dispatchServlet, null);
			fail("Exception expected.");
		} catch (Exception e) {
			// as expected
		}
		
	}
	

}
