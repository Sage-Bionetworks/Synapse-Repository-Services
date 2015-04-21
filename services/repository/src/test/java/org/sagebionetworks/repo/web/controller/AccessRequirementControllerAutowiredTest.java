package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
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
public class AccessRequirementControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	// Used for cleanup
	@Autowired
	private EntityService entityController;
	
	@Autowired
	private UserManager userManager;

	private Evaluation evaluation;

	static private Log log = LogFactory
			.getLog(AccessRequirementControllerAutowiredTest.class);

	private Long userId;
	private UserInfo otherUserInfo;
	private UserInfo testUser;
	private Project project;

	private List<String> toDelete;

	@Before
	public void before() throws Exception {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		otherUserInfo = userManager.getUserInfo(userManager.createUser(user));
		
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(testUser);
		project = new Project();
		project.setName("createAtLeastOneOfEachType");
		project = servletTestHelper.createEntity(dispatchServlet, project, userId);
		assertNotNull(project);
		toDelete.add(project.getId());

		evaluation = new Evaluation();
		evaluation.setName("name");
		evaluation.setContentSource(project.getId());
		evaluation.setDescription("description");
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation = entityServletHelper.createEvaluation(evaluation, userId);
	}

	@After
	public void after() throws Exception {
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
		
		if (evaluation!=null) {
			try {
				entityServletHelper.deleteEvaluation(evaluation.getId(), userId);
			} catch (Exception e) {}
		}
		
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		userManager.deletePrincipal(adminUserInfo, otherUserInfo.getId());
	}

	private static AccessRequirement newAccessRequirement(ACCESS_TYPE accessType) {
		TermsOfUseAccessRequirement dto = new TermsOfUseAccessRequirement();
		dto.setConcreteType(dto.getClass().getName());
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
		AccessRequirement clone = servletTestHelper.createAccessRequirement(
				 dispatchServlet, accessRequirement, userId, extraParams);
		assertNotNull(clone);

		// test getAccessRequirementsForEntity
		PaginatedResults<AccessRequirement> results = servletTestHelper.getEntityAccessRequirements(
				dispatchServlet, entityId, userId);	
		List<AccessRequirement> ars = results.getResults();
		assertEquals(1, ars.size());
		
		// get the unmet access requirements for the entity, 
		// when the user is the entity owner (should be one)
		results = servletTestHelper.getUnmetEntityAccessRequirements(
				dispatchServlet, entityId, userId, ACCESS_TYPE.DOWNLOAD);	
		ars = results.getResults();
		assertEquals(1, ars.size());
		
		// get the unmet access requirements for the entity
		results = servletTestHelper.getUnmetEntityAccessRequirements(
				dispatchServlet, entityId, otherUserInfo.getId(), ACCESS_TYPE.DOWNLOAD);	
		ars = results.getResults();
		assertEquals(1, ars.size());
		
		// if we don't specify what kind of access requirement we want, then the
		// service should assume we want DOWNLOAD access requirements
		results = servletTestHelper.getUnmetEntityAccessRequirements(
				dispatchServlet, entityId, otherUserInfo.getId(), null);	
		ars = results.getResults();
		assertEquals(1, ars.size());
		
		TermsOfUseAccessRequirement tou = (TermsOfUseAccessRequirement)clone;
		tou.setTermsOfUse("bar");
		AccessRequirement updated = servletTestHelper.updateAccessRequirement(
				 dispatchServlet, tou, userId, extraParams);
		tou.setEtag(updated.getEtag());
		tou.setModifiedOn(updated.getModifiedOn());
		assertEquals(tou, updated);
		
		// test deletion
		servletTestHelper.deleteAccessRequirements(dispatchServlet, ars.get(0).getId().toString(), userId);
		
		results = servletTestHelper.getEntityAccessRequirements(
				dispatchServlet, entityId, userId);	
		ars = results.getResults();
		assertEquals(0, ars.size());
	}
	
	@Test
	public void testEvaluationRequirementRoundTrip() throws Exception {
		// create a new access requirement
		AccessRequirement accessRequirement = null;
		Map<String, String> extraParams = new HashMap<String, String>();
		accessRequirement = newAccessRequirement(ACCESS_TYPE.SUBMIT);
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(evaluation.getId());
		subjectId.setType(RestrictableObjectType.EVALUATION);
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId})); 
		AccessRequirement clone = servletTestHelper.createAccessRequirement(
				 dispatchServlet, accessRequirement, userId, extraParams);
		assertNotNull(clone);

		// test getAccessRequirementsForEvaluation
		PaginatedResults<AccessRequirement> results = servletTestHelper.getEvaluationAccessRequirements(
				dispatchServlet, evaluation.getId(), userId);	
		List<AccessRequirement> ars = results.getResults();
		assertEquals(1, ars.size());
		
		// get the unmet access requirements for the evaluation, 
		// when the user is the entity owner, should be the same as for others
		results = servletTestHelper.getUnmetEvaluationAccessRequirements(
				dispatchServlet, evaluation.getId(), userId, ACCESS_TYPE.SUBMIT);	
		ars = results.getResults();
		assertEquals(1, ars.size());
		
		// get the unmet access requirements for the evaluation
		results = servletTestHelper.getUnmetEvaluationAccessRequirements(
				dispatchServlet, evaluation.getId(), Long.parseLong(otherUserInfo.getId().toString()), ACCESS_TYPE.SUBMIT);	
		ars = results.getResults();
		assertEquals(1, ars.size());
		
		// get the unmet access requirements for the evaluation when accessType is omitted
		results = servletTestHelper.getUnmetEvaluationAccessRequirements(
				dispatchServlet, evaluation.getId(), Long.parseLong(otherUserInfo.getId().toString()), null);	
		ars = results.getResults();
		assertEquals(1, ars.size());
		
		TermsOfUseAccessRequirement tou = (TermsOfUseAccessRequirement)clone;
		tou.setTermsOfUse("bar");
		AccessRequirement updated = servletTestHelper.updateAccessRequirement(
				 dispatchServlet, tou, userId, extraParams);
		tou.setEtag(updated.getEtag());
		tou.setModifiedOn(updated.getModifiedOn());
		assertEquals(tou, updated);
		
		// test deletion
		servletTestHelper.deleteAccessRequirements(dispatchServlet, ars.get(0).getId().toString(), userId);
		
		results = servletTestHelper.getEvaluationAccessRequirements(
				dispatchServlet, evaluation.getId(), userId);	
		ars = results.getResults();
		assertEquals(0, ars.size());
	}


}
