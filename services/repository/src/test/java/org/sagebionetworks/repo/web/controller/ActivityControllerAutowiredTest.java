package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This is an integration test for the ActivityController.
 * 
 * @author dburdick
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ActivityControllerAutowiredTest {

	// Used for cleanup
	@Autowired
	private ActivityService activityService;
	
	@Autowired
	private UserManager userManager;

	static private Log log = LogFactory.getLog(ActivityControllerAutowiredTest.class);

	private static HttpServlet dispatchServlet;
	
	private String userId = TestUserDAO.ADMIN_USER_NAME;
	private UserInfo testUser;

	private List<String> toDelete;
	
	@Before
	public void before() throws Exception {		
		assertNotNull(activityService);
		toDelete = new ArrayList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(testUser);
	}

	@After
	public void after() throws UnauthorizedException {
		if (activityService != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					activityService.deleteActivity(userId, idToDelete);
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
		dispatchServlet = DispatchServletSingleton.getInstance();
	}
	
	@Test
	public void testCRUD() throws Exception {
		// create activity
		Activity act;
		act = new Activity();
		act.setDescription("some desc");
		Map<String, String> extraParams = new HashMap<String, String>();
		act = ServletTestHelper.createActivity(dispatchServlet, act, userId, extraParams);
		assertNotNull(act);
		toDelete.add(act.getId());
		
		// get activity
		Activity getAct = ServletTestHelper.getActivity(dispatchServlet, act.getId(), userId);
		assertEquals(act.getId(), getAct.getId());
		assertEquals(act.getDescription(), getAct.getDescription());

		// test update
		String updatedDesc = "updated Desc";
		act.setDescription(updatedDesc);
		Activity updatedAct = ServletTestHelper.updateActivity(dispatchServlet, act, userId, extraParams);
		assertEquals(act.getId(), updatedAct.getId());
		assertEquals(updatedDesc, updatedAct.getDescription());
		
		// test deletion
		ServletTestHelper.deleteActivity(dispatchServlet, act.getId(), userId, extraParams);
		// assure deletion
		try {
			ServletTestHelper.getActivity(dispatchServlet, act.getId(), userId);
			fail("Activity should have been deleted");
		} catch (ServletTestHelperException e) {
			// good.
		}
	}

}
