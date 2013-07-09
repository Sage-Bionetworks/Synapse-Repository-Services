package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessRequirementManagerImplAutoWiredTest {
	@Autowired
	public NodeManager nodeManager;
	@Autowired
	public UserProvider testUserProvider;
	
	@Autowired
	public AccessRequirementManager accessRequirementManager;
	
	@Autowired
	EvaluationManager evaluationManager;
	
	private UserInfo adminUserInfo;
	
	private static final String TERMS_OF_USE = "my dog has fleas";

	private List<String> nodesToDelete;
	
	private String entityId;
	
	private Evaluation evaluation;
	private Evaluation adminEvaluation;
	
	AccessRequirement ar;

	@Before
	public void before() throws Exception {
		adminUserInfo = testUserProvider.getTestAdminUserInfo();
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
		
		Node rootProject = new Node();
		rootProject.setName("root "+System.currentTimeMillis());
		rootProject.setNodeType(EntityType.project.name());
		String rootId = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId); // the deletion of 'rootId' will cascade to its children
		Node node = new Node();
		node.setName("A");
		node.setNodeType(EntityType.layer.name());
		node.setParentId(rootId);
		entityId = nodeManager.createNewNode(node, adminUserInfo);
		
		evaluation = newEvaluation("test-name", testUserProvider.getTestUserInfo(), rootId);
		adminEvaluation = newEvaluation("admin-name", testUserProvider.getTestAdminUserInfo(), rootId);
	}
	
	private Evaluation newEvaluation(String name, UserInfo userInfo, String contentSource) throws NotFoundException {
		Evaluation evaluation = new Evaluation();
		evaluation.setName(name);
		evaluation.setCreatedOn(new Date());
		evaluation.setContentSource(contentSource);
		evaluation.setDescription("description");
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation = evaluationManager.createEvaluation(userInfo, evaluation);
		return evaluation;
	}
	
	@After
	public void after() throws Exception {
		if(nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					nodeManager.delete(adminUserInfo, id);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
			}
		}
		
		if (ar!=null && ar.getId()!=null && accessRequirementManager!=null) {
			accessRequirementManager.deleteAccessRequirement(adminUserInfo, ar.getId().toString());
		}
		
		if (evaluation!=null) {
			try {
				evaluationManager.deleteEvaluation(testUserProvider.getTestAdminUserInfo(), evaluation.getId());
				evaluation=null;
			} catch (Exception e) {}
		}
		if (adminEvaluation!=null) {
			try {
				evaluationManager.deleteEvaluation(testUserProvider.getTestAdminUserInfo(), adminEvaluation.getId());
				adminEvaluation=null;
			} catch (Exception e) {}
		}
	}
	
	private static TermsOfUseAccessRequirement newEntityAccessRequirement(String entityId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setEntityType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}
	
	private static TermsOfUseAccessRequirement newEvaluationAccessRequirement(String evaluationId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(evaluationId);
		rod.setType(RestrictableObjectType.EVALUATION);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setEntityType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.PARTICIPATE);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}
	
	@Test
	public void testCreateEntityAccessRequirement() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		assertNotNull(ar.getCreatedBy());
		assertNotNull(ar.getCreatedOn());
		assertNotNull(ar.getSubjectIds());
		assertNotNull(ar.getId());
		assertNotNull(ar.getModifiedBy());
		assertNotNull(ar.getModifiedOn());
	}
	
	@Test
	public void testCreateLockAccessRequirement() throws Exception {
		ar = AccessRequirementManagerImpl.newLockAccessRequirement(adminUserInfo, entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		assertNotNull(ar.getCreatedBy());
		assertNotNull(ar.getCreatedOn());
		assertNotNull(ar.getSubjectIds());
		assertNotNull(ar.getId());
		assertNotNull(ar.getModifiedBy());
		assertNotNull(ar.getModifiedOn());
	}
	
	@Test
	public void testCreateEvaluationAccessRequirement() throws Exception {
		ar = newEvaluationAccessRequirement(evaluation.getId());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		assertNotNull(ar.getCreatedBy());
		assertNotNull(ar.getCreatedOn());
		assertNotNull(ar.getSubjectIds());
		assertNotNull(ar.getId());
		assertNotNull(ar.getModifiedBy());
		assertNotNull(ar.getModifiedOn());
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessRequirementBadParam1() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar.setSubjectIds(null);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessRequirementBadParam2() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar.setEntityType(ACTAccessRequirement.class.getName());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessRequirementBadParam3() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar.setAccessType(null);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testEntityCreateAccessRequirementForbidden() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(testUserProvider.getTestUserInfo(), ar);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testEvaluationCreateAccessRequirementForbidden() throws Exception {
		ar = newEvaluationAccessRequirement(adminEvaluation.getId());
		// this user will not have permission to add a restriction to the evaluation
		ar = accessRequirementManager.createAccessRequirement(testUserProvider.getTestUserInfo(), ar);
	}
	
	@Test
	public void testUpdateAccessRequirement() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);

		// ensure that the 'modifiedOn' date is later
		Thread.sleep(100L);
		long arModifiedOn = ar.getModifiedOn().getTime();
		ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>()); // change the entity id list
		AccessRequirement ar2 = accessRequirementManager.updateAccessRequirement(adminUserInfo, ar);
		assertTrue(ar2.getModifiedOn().getTime()-arModifiedOn>0);
		assertTrue(ar2.getSubjectIds().isEmpty());
	}
	
	@Test
	public void testGetAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	@Test
	public void testGetUnmetEntityAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		UserInfo otherUserInfo = testUserProvider.getTestUserInfo();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(otherUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	@Test
	public void testGetUnmetEvaluationAccessRequirements() throws Exception {
		ar = newEvaluationAccessRequirement(adminEvaluation.getId());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		UserInfo otherUserInfo = testUserProvider.getTestUserInfo();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(adminEvaluation.getId());
		rod.setType(RestrictableObjectType.EVALUATION);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(otherUserInfo, rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	// entity owner never has unmet access requirements
	@Test
	public void testGetUnmetEntityAccessRequirementsOwner() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(adminUserInfo, rod);
		assertEquals(0L, ars.getTotalNumberOfResults());
		assertEquals(0, ars.getResults().size());
	}
	
	// evaluation owner gets no special treatment
	@Test
	public void testGetUnmetEvaluationAccessRequirementsOwner() throws Exception {
		ar = newEvaluationAccessRequirement(evaluation.getId());
		ar = accessRequirementManager.createAccessRequirement(testUserProvider.getTestUserInfo(), ar);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(evaluation.getId());
		rod.setType(RestrictableObjectType.EVALUATION);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getUnmetAccessRequirements(testUserProvider.getTestUserInfo(), rod);
		assertEquals(1L, ars.getTotalNumberOfResults());
		assertEquals(1, ars.getResults().size());
	}
	
	@Test
	public void testDeleteAccessRequirements() throws Exception {
		ar = newEntityAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		accessRequirementManager.deleteAccessRequirement(adminUserInfo, ar.getId().toString());
		ar=null;
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessRequirement> ars = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo, rod);
		assertEquals(0L, ars.getTotalNumberOfResults());
		assertEquals(0, ars.getResults().size());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testDeleteEvaluationAccessRequirementForbidden() throws Exception {
		ar = newEvaluationAccessRequirement(adminEvaluation.getId());
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		accessRequirementManager.deleteAccessRequirement(testUserProvider.getTestUserInfo(), ar.getId().toString());
	}
	

}
