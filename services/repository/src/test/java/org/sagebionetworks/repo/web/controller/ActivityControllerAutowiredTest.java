package org.sagebionetworks.repo.web.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.ActivityService;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is an integration test for the ActivityController.
 * 
 * @author dburdick
 * 
 */
public class ActivityControllerAutowiredTest extends AbstractAutowiredControllerJunit5TestBase {

	// Used for cleanup
	@Autowired
	private ActivityService activityService;
	
	@Autowired 
	private EntityService entityService;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;
	
	@Autowired
	private OpenIDConnectManager oidcManager;
	
	private Long userId;

	private List<String> activityIdstoDelete;
	
	private List<String> entityIdsToDelete;
	
	HttpServletRequest mockRequest;
	
	private UserInfo userInfo;
	
	@BeforeEach
	public void before() throws Exception {		
		assertNotNull(activityService);
		activityIdstoDelete = new ArrayList<String>();
		entityIdsToDelete = new ArrayList<String>();
		
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		String accessToken = oidcTokenHelper.createTotalAccessToken(userId);
		userInfo = oidcManager.getUserAuthorization(accessToken);
		
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		when(mockRequest.getParameter(eq(AuthorizationConstants.USER_ID_PARAM))).thenReturn(userId.toString());
	}

	@AfterEach
	public void after() throws UnauthorizedException {

		if (entityService != null && entityIdsToDelete != null) {
			for (String idToDelete : entityIdsToDelete) {
				try {
					entityService.deleteEntity(userId, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
		
		if (activityService != null && activityIdstoDelete != null) {
			for (String idToDelete : activityIdstoDelete) {
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

	@Test
	public void testCRUD() throws Exception {
		// create activity
		Activity act;
		act = new Activity();
		act.setDescription("some desc");
		Map<String, String> extraParams = new HashMap<String, String>();
		act = servletTestHelper.createActivity(dispatchServlet, act, userId, extraParams);
		assertNotNull(act);
		activityIdstoDelete.add(act.getId());
		
		// get activity
		Activity getAct = servletTestHelper.getActivity(dispatchServlet, act.getId(), userId);
		assertEquals(act.getId(), getAct.getId());
		assertEquals(act.getDescription(), getAct.getDescription());

		// test update
		String updatedDesc = "updated Desc";
		act.setDescription(updatedDesc);
		Activity updatedAct = servletTestHelper.updateActivity(dispatchServlet, act, userId, extraParams);
		assertEquals(act.getId(), updatedAct.getId());
		assertEquals(updatedDesc, updatedAct.getDescription());
		
		// test deletion
		servletTestHelper.deleteActivity(dispatchServlet, act.getId(), userId, extraParams);
		// assure deletion
		try {
			servletTestHelper.getActivity(dispatchServlet, act.getId(), userId);
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
		act = servletTestHelper.createActivity(dispatchServlet, act, userId, extraParams);
		assertNotNull(act);
		activityIdstoDelete.add(act.getId());

		// add some entities generated by the activity
		Project proj = new Project();
		proj = entityService.createEntity(userInfo, proj, null);
		entityIdsToDelete.add(proj.getId());
		TableEntity entity1 = new TableEntity();
		entity1.setParentId(proj.getId());
		entity1 = entityService.createEntity(userInfo, entity1, act.getId());
		entityIdsToDelete.add(entity1.getId());
		TableEntity entity2 = new TableEntity();
		entity2.setParentId(proj.getId());
		entity2 = entityService.createEntity(userInfo, entity2, act.getId());
		entityIdsToDelete.add(entity2.getId());
		
		// query for generated by
		PaginatedResults<Reference> generated;
		extraParams = new HashMap<String, String>();
		extraParams.put("offset", "0");
		extraParams.put("limit", Integer.toString(Integer.MAX_VALUE));
		generated = servletTestHelper.getEntitiesGeneratedBy(dispatchServlet, act, userId, extraParams);
		
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
