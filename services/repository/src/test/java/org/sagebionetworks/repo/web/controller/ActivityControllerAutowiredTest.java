package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.ActivityService;
import org.sagebionetworks.repo.web.service.EntityService;
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
	private EntityService entityService;
	
	@Autowired
	private UserManager userManager;

	private static HttpServlet dispatchServlet;
	
	private String userName;

	private List<String> activityIdstoDelete;
	
	private List<String> entityIdsToDelete;
	
	HttpServletRequest mockRequest;
	
	@Before
	public void before() throws Exception {		
		assertNotNull(activityService);
		activityIdstoDelete = new ArrayList<String>();
		entityIdsToDelete = new ArrayList<String>();
		
		userName = userManager.getGroupName(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
	}

	@After
	public void after() throws UnauthorizedException {
		if (activityService != null && activityIdstoDelete != null) {
			for (String idToDelete : activityIdstoDelete) {
				try {
					activityService.deleteActivity(userName, idToDelete);
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
					entityService.deleteEntity(userName, idToDelete);
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
		act = ServletTestHelper.createActivity(dispatchServlet, act, userName, extraParams);
		assertNotNull(act);
		activityIdstoDelete.add(act.getId());
		
		// get activity
		Activity getAct = ServletTestHelper.getActivity(dispatchServlet, act.getId(), userName);
		assertEquals(act.getId(), getAct.getId());
		assertEquals(act.getDescription(), getAct.getDescription());

		// test update
		String updatedDesc = "updated Desc";
		act.setDescription(updatedDesc);
		Activity updatedAct = ServletTestHelper.updateActivity(dispatchServlet, act, userName, extraParams);
		assertEquals(act.getId(), updatedAct.getId());
		assertEquals(updatedDesc, updatedAct.getDescription());
		
		// test deletion
		ServletTestHelper.deleteActivity(dispatchServlet, act.getId(), userName, extraParams);
		// assure deletion
		try {
			ServletTestHelper.getActivity(dispatchServlet, act.getId(), userName);
			fail("Activity should have been deleted");
		} catch (NotFoundException e) {
			// good.
		}
	}

	@Test
	public void testGetEntitiesGeneratedBy() throws Exception {
		// create activity
		Activity act;
		act = new Activity();
		act.setDescription("some desc");
		Map<String, String> extraParams = new HashMap<String, String>();
		act = ServletTestHelper.createActivity(dispatchServlet, act, userName, extraParams);
		assertNotNull(act);
		activityIdstoDelete.add(act.getId());

		// add some entities generated by the activity
		Project proj = new Project();
		proj.setEntityType(Project.class.getName());
		proj = entityService.createEntity(userName, proj, null, mockRequest);
		entityIdsToDelete.add(proj.getId());
		Data entity1 = new Data();
		entity1.setEntityType(Data.class.getName());
		entity1.setParentId(proj.getId());
		entity1 = entityService.createEntity(userName, entity1, act.getId(), mockRequest);
		entityIdsToDelete.add(entity1.getId());
		Data entity2 = new Data();
		entity2.setEntityType(Data.class.getName());
		entity2.setParentId(proj.getId());
		entity2 = entityService.createEntity(userName, entity2, act.getId(), mockRequest);
		entityIdsToDelete.add(entity2.getId());
		
		// query for generated by
		PaginatedResults<Reference> generated;
		extraParams = new HashMap<String, String>();
		extraParams.put("offset", "0");
		extraParams.put("limit", Integer.toString(Integer.MAX_VALUE));
		generated = ServletTestHelper.getEntitiesGeneratedBy(dispatchServlet, act, userName, extraParams);
		
		// verify
		assertNotNull(generated);
		assertEquals(2, generated.getTotalNumberOfResults());
		List<Reference> refs = generated.getResults();
		
		Reference ref1 = new Reference();
		ref1.setTargetId(entity1.getId());
		ref1.setTargetVersionNumber(entity1.getVersionNumber());
		assertTrue(refs.contains(ref1));

		Reference ref2 = new Reference();
		ref2.setTargetId(entity2.getId());
		ref2.setTargetVersionNumber(entity2.getVersionNumber());
		assertTrue(refs.contains(ref2));

	}
	
}
