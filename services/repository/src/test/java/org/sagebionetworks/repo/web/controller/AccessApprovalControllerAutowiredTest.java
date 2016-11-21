package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a an integration test for the AccessRequirementController.
 * 
 * @author jmhill, adapted by bhoff
 * 
 */
public class AccessApprovalControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	// Used for cleanup
	@Autowired
	private EntityService entityController;
	
	@Autowired
	private UserManager userManager;

	private Long userId;
	private UserInfo testUser;
	private Project project;
	private Evaluation evaluation;

	private List<String> toDelete;
	
	private AccessRequirement entityAccessRequirement = null;
	private AccessRequirement evaluationAccessRequirement = null;
	
	private static AccessRequirement newAccessRequirement() {
		TermsOfUseAccessRequirement dto = new TermsOfUseAccessRequirement();
		dto.setConcreteType(dto.getClass().getName());
		dto.setAccessType(ACCESS_TYPE.DOWNLOAD);
		dto.setTermsOfUse("foo");
		return dto;
	}
	
	@Before
	public void before() throws Exception {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(testUser);
		project = new Project();
		project.setName("createAtLeastOneOfEachType");
		project = servletTestHelper.createEntity(dispatchServlet, project, userId);
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
		entityAccessRequirement = servletTestHelper.createAccessRequirement(
				 dispatchServlet, entityAccessRequirement, userId, extraParams);
		
		evaluation = new Evaluation();
		evaluation.setName("name");
		evaluation.setContentSource(project.getId());
		evaluation.setDescription("description");
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation = entityServletHelper.createEvaluation(evaluation, userId);
		
		evaluationAccessRequirement = newAccessRequirement();
		subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(evaluation.getId());
		subjectId.setType(RestrictableObjectType.EVALUATION);
		evaluationAccessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId})); 
		evaluationAccessRequirement = servletTestHelper.createAccessRequirement(
				 dispatchServlet, evaluationAccessRequirement, userId, extraParams);
		
	}

	@After
	public void after() throws Exception {
		servletTestHelper.deleteAccessRequirements(dispatchServlet, entityAccessRequirement.getId().toString(), userId);
		if (entityController != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					entityController.deleteEntity(userId, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
		servletTestHelper.deleteAccessRequirements(dispatchServlet, evaluationAccessRequirement.getId().toString(), userId);
		try {
			entityServletHelper.deleteEvaluation(evaluation.getId(), userId);
		} catch (Exception e) {
		}
	}

	private static TermsOfUseAccessApproval newToUAccessApproval(Long requirementId, String accessorId) {
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(accessorId);
		aa.setConcreteType(TermsOfUseAccessApproval.class.getName());
		aa.setRequirementId(requirementId);
		return aa;
	}
	
	@Test
	public void testEntityAccessApprovalRoundTrip() throws Exception {
		// create a new access approval
		Map<String, String> extraParams = new HashMap<String, String>();
		AccessApproval accessApproval = newToUAccessApproval(entityAccessRequirement.getId(), testUser.getId().toString());
		String entityId = project.getId();
		AccessApproval clone = servletTestHelper.createAccessApproval(
				 dispatchServlet, accessApproval, userId, extraParams);
		assertNotNull(clone);

		// test getAccessApprovals for the entity
		PaginatedResults<AccessApproval> results = servletTestHelper.getEntityAccessApprovals(
				dispatchServlet, entityId, userId);	
		List<AccessApproval> ars = results.getResults();
		assertEquals(1, ars.size());
		
		// test deletion
		servletTestHelper.deleteAccessApproval(dispatchServlet, ars.get(0).getId().toString(), userId);

		// test deletion using access requirementId and accessorId
		try {
			servletTestHelper.deleteAccessApprovals(dispatchServlet, userId, entityAccessRequirement.getId().toString(), testUser.getId().toString());
			fail("Expecting IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// The service is wired up.
			// Exception thrown for not supporting access approval deletion for TermOfUseAccessRequirement
		}
		
		results = servletTestHelper.getEntityAccessApprovals(
				dispatchServlet, entityId, userId);	
		ars = results.getResults();
		assertEquals(0, ars.size());
	}

	@Test
	public void testEvaluationAccessApprovalRoundTrip() throws Exception {
		// create a new access approval
		Map<String, String> extraParams = new HashMap<String, String>();
		AccessApproval accessApproval = newToUAccessApproval(evaluationAccessRequirement.getId(), testUser.getId().toString());
		AccessApproval clone = servletTestHelper.createAccessApproval(
				 dispatchServlet, accessApproval, userId, extraParams);
		assertNotNull(clone);

		// test getAccessApprovals for the entity
		PaginatedResults<AccessApproval> results = servletTestHelper
				.getEvaluationAccessApprovals(
				dispatchServlet, evaluation.getId(), userId);
		List<AccessApproval> ars = results.getResults();
		assertEquals(1, ars.size());
		
		// test deletion
		servletTestHelper.deleteAccessApproval(dispatchServlet, ars.get(0).getId().toString(), userId);
		
		results = servletTestHelper.getEvaluationAccessApprovals(
				dispatchServlet, evaluation.getId(), userId);
		ars = results.getResults();
		assertEquals(0, ars.size());
	}

}
