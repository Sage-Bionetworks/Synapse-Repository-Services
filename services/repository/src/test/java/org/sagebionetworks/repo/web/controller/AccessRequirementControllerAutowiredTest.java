package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This is a an integration test for the AccessRequirementController.
 * 
 * @author jmhill, adapted by bhoff
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessRequirementControllerAutowiredTest {

	// Used for cleanup
	@Autowired
	private EntityService entityController;
	
	@Autowired
	private UserManager userManager;

	private Evaluation evaluation;

	static private Log log = LogFactory
			.getLog(AccessRequirementControllerAutowiredTest.class);

	private static HttpServlet dispatchServlet;
	
	private String userName = TestUserDAO.ADMIN_USER_NAME;
	private UserInfo testUser;
	private Project project;

	private List<String> toDelete;

	@Before
	public void before() throws Exception {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userName);
		UserInfo.validateUserInfo(testUser);
		project = new Project();
		project.setName("createAtLeastOneOfEachType");
		project = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(project);
		toDelete.add(project.getId());

		evaluation = new Evaluation();
		evaluation.setName("name");
		evaluation.setContentSource(project.getId());
		evaluation.setDescription("description");
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation = (new EntityServletTestHelper()).createEvaluation(evaluation, userName);
	}

	@After
	public void after() throws Exception {
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
		
		if (evaluation!=null) {
			try {
				(new EntityServletTestHelper()).deleteEvaluation(evaluation.getId(), userName);
			} catch (Exception e) {}
		}
	}

	@BeforeClass
	public static void beforeClass() throws ServletException {
		dispatchServlet = DispatchServletSingleton.getInstance();
	}


	private static AccessRequirement newAccessRequirement(ACCESS_TYPE accessType) {
		TermsOfUseAccessRequirement dto = new TermsOfUseAccessRequirement();
		dto.setEntityType(dto.getClass().getName());
		dto.setAccessType(accessType);
		dto.setTermsOfUse("foo");
		return dto;
	}
	
	@Test
	public void testEntityRequirementRoundTrip() throws Exception {
		// create a new access requirement
		AccessRequirement accessRequirement = null;
		Map<String, String> extraParams = new HashMap<String, String>();
		accessRequirement = newAccessRequirement(ACCESS_TYPE.DOWNLOAD);
		String entityId = project.getId();
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId})); 
		AccessRequirement clone = ServletTestHelper.createAccessRequirement(
				 dispatchServlet, accessRequirement, userName, extraParams);
		assertNotNull(clone);

		// test getAccessRequirementsForEntity
		PaginatedResults<AccessRequirement> results = ServletTestHelper.getEntityAccessRequirements(
				dispatchServlet, entityId, userName);	
		List<AccessRequirement> ars = results.getResults();
		assertEquals(1, ars.size());
		
		// get the unmet access requirements for the entity, 
		// when the user is the entity owner (should be none)
		results = ServletTestHelper.getUnmetEntityAccessRequirements(
				dispatchServlet, entityId, userName);	
		ars = results.getResults();
		assertEquals(0, ars.size());
		
		// get the unmet access requirements for the entity
		results = ServletTestHelper.getUnmetEntityAccessRequirements(
				dispatchServlet, entityId, TestUserDAO.TEST_USER_NAME);	
		ars = results.getResults();
		assertEquals(1, ars.size());
		
		// test deletion
		ServletTestHelper.deleteAccessRequirements(dispatchServlet, ars.get(0).getId().toString(), userName);
		
		results = ServletTestHelper.getEntityAccessRequirements(
				dispatchServlet, entityId, userName);	
		ars = results.getResults();
		assertEquals(0, ars.size());
	}
	
	@Test
	public void testEvaluationRequirementRoundTrip() throws Exception {
		// create a new access requirement
		AccessRequirement accessRequirement = null;
		Map<String, String> extraParams = new HashMap<String, String>();
		accessRequirement = newAccessRequirement(ACCESS_TYPE.PARTICIPATE);
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(evaluation.getId());
		subjectId.setType(RestrictableObjectType.EVALUATION);
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId})); 
		AccessRequirement clone = ServletTestHelper.createAccessRequirement(
				 dispatchServlet, accessRequirement, userName, extraParams);
		assertNotNull(clone);

		// test getAccessRequirementsForEvaluation
		PaginatedResults<AccessRequirement> results = ServletTestHelper.getEvaluationAccessRequirements(
				dispatchServlet, evaluation.getId(), userName);	
		List<AccessRequirement> ars = results.getResults();
		assertEquals(1, ars.size());
		
		// get the unmet access requirements for the evaluation, 
		// when the user is the entity owner, should be the same as for others
		results = ServletTestHelper.getUnmetEvaluationAccessRequirements(
				dispatchServlet, evaluation.getId(), userName);	
		ars = results.getResults();
		assertEquals(1, ars.size());
		
		// get the unmet access requirements for the evaluation
		results = ServletTestHelper.getUnmetEvaluationAccessRequirements(
				dispatchServlet, evaluation.getId(), TestUserDAO.TEST_USER_NAME);	
		ars = results.getResults();
		assertEquals(1, ars.size());
		
		// test deletion
		ServletTestHelper.deleteAccessRequirements(dispatchServlet, ars.get(0).getId().toString(), userName);
		
		results = ServletTestHelper.getEvaluationAccessRequirements(
				dispatchServlet, evaluation.getId(), userName);	
		ars = results.getResults();
		assertEquals(0, ars.size());
	}


}
