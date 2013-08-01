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
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
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
public class AccessApprovalControllerAutowiredTest {

	// Used for cleanup
	@Autowired
	private EntityService entityController;
	
	@Autowired
	private UserManager userManager;

	static private Log log = LogFactory
			.getLog(AccessApprovalControllerAutowiredTest.class);

	private static HttpServlet dispatchServlet;
	
	private String userName = TestUserDAO.ADMIN_USER_NAME;
	private UserInfo testUser;
	private Project project;
	private Evaluation evaluation;

	private List<String> toDelete;
	
	private AccessRequirement entityAccessRequirement = null;
	private AccessRequirement evaluationAccessRequirement = null;
	
	private EntityServletTestHelper entityServletTestHelper = null;

	private static AccessRequirement newAccessRequirement() {
		TermsOfUseAccessRequirement dto = new TermsOfUseAccessRequirement();
		dto.setEntityType(dto.getClass().getName());
		dto.setAccessType(ACCESS_TYPE.DOWNLOAD);
		dto.setTermsOfUse("foo");
		return dto;
	}
	
	@Before
	public void before() throws Exception {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		
		entityServletTestHelper = new EntityServletTestHelper();
		
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userName);
		UserInfo.validateUserInfo(testUser);
		project = new Project();
		project.setName("createAtLeastOneOfEachType");
		project = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(project);
		toDelete.add(project.getId());

		// create a new access requirement
		Map<String, String> extraParams = new HashMap<String, String>();
		entityAccessRequirement = newAccessRequirement();
		String entityId = project.getId();
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		entityAccessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId})); 
		entityAccessRequirement = ServletTestHelper.createAccessRequirement(
				 dispatchServlet, entityAccessRequirement, userName, extraParams);
		
		evaluation = new Evaluation();
		evaluation.setName("name");
		evaluation.setContentSource(project.getId());
		evaluation.setDescription("description");
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation = entityServletTestHelper.createEvaluation(evaluation, userName);
		
		evaluationAccessRequirement = newAccessRequirement();
		subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(evaluation.getId());
		subjectId.setType(RestrictableObjectType.EVALUATION);
		evaluationAccessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId})); 
		evaluationAccessRequirement = ServletTestHelper.createAccessRequirement(
				 dispatchServlet, evaluationAccessRequirement, userName, extraParams);
		
	}

	@After
	public void after() throws Exception {
		ServletTestHelper.deleteAccessRequirements(dispatchServlet, entityAccessRequirement.getId().toString(), userName);
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
		ServletTestHelper.deleteAccessRequirements(dispatchServlet, evaluationAccessRequirement.getId().toString(), userName);
		try {
			entityServletTestHelper.deleteEvaluation(evaluation.getId(), userName);
		} catch (Exception e) {
		}
	}

	@BeforeClass
	public static void beforeClass() throws ServletException {
		dispatchServlet = DispatchServletSingleton.getInstance();
	}


	
	private static TermsOfUseAccessApproval newToUAccessApproval(Long requirementId, String accessorId) {
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(accessorId);
		aa.setEntityType(TermsOfUseAccessApproval.class.getName());
		aa.setRequirementId(requirementId);
		return aa;
	}
	
	@Test
	public void testEntityAccessApprovalRoundTrip() throws Exception {
		// create a new access approval
		Map<String, String> extraParams = new HashMap<String, String>();
		AccessApproval accessApproval = newToUAccessApproval(entityAccessRequirement.getId(), testUser.getIndividualGroup().getId());
		String entityId = project.getId();
		AccessApproval clone = ServletTestHelper.createAccessApproval(
				 dispatchServlet, accessApproval, userName, extraParams);
		assertNotNull(clone);

		// test getAccessApprovals for the entity
		PaginatedResults<AccessApproval> results = ServletTestHelper.getEntityAccessApprovals(
				dispatchServlet, entityId, userName);	
		List<AccessApproval> ars = results.getResults();
		assertEquals(1, ars.size());
		
		// test deletion
		ServletTestHelper.deleteAccessApprovals(dispatchServlet, ars.get(0).getId().toString(), userName);
		
		results = ServletTestHelper.getEntityAccessApprovals(
				dispatchServlet, entityId, userName);	
		ars = results.getResults();
		assertEquals(0, ars.size());
	}

	@Test
	public void testEvaluationAccessApprovalRoundTrip() throws Exception {
		// create a new access approval
		Map<String, String> extraParams = new HashMap<String, String>();
		AccessApproval accessApproval = newToUAccessApproval(evaluationAccessRequirement.getId(), testUser.getIndividualGroup().getId());
		AccessApproval clone = ServletTestHelper.createAccessApproval(
				 dispatchServlet, accessApproval, userName, extraParams);
		assertNotNull(clone);

		// test getAccessApprovals for the entity
		PaginatedResults<AccessApproval> results = ServletTestHelper.getEvaluationAccessApprovals(
				dispatchServlet, evaluation.getId(), userName);	
		List<AccessApproval> ars = results.getResults();
		assertEquals(1, ars.size());
		
		// test deletion
		ServletTestHelper.deleteAccessApprovals(dispatchServlet, ars.get(0).getId().toString(), userName);
		
		results = ServletTestHelper.getEvaluationAccessApprovals(
				dispatchServlet, evaluation.getId(), userName);	
		ars = results.getResults();
		assertEquals(0, ars.size());
	}

}
