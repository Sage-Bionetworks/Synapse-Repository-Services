package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessRequirementDAOImplTest {

	@Autowired
	UserGroupDAO userGroupDAO;

	@Autowired
	AccessRequirementDAO accessRequirementDAO;

	@Autowired
	NodeDAO nodeDAO;

	@Autowired
	EvaluationDAO evaluationDAO;
	
	@Autowired
	private IdGenerator idGenerator;

	private static final String TEST_USER_NAME = "test-user";
	
	private UserGroup individualGroup = null;
	private Node node = null;
	private Node node2 = null;
	private TermsOfUseAccessRequirement accessRequirement = null;
	private TermsOfUseAccessRequirement accessRequirement2 = null;
	private List<AccessRequirement> ars = null;
	
	private Evaluation evaluation = null;
	private TermsOfUseAccessRequirement accessRequirement3 = null;
	private TermsOfUseAccessRequirement accessRequirement4 = null;

	
	@Before
	public void setUp() throws Exception {
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup == null) {
			individualGroup = new UserGroup();
			individualGroup.setName(TEST_USER_NAME);
			individualGroup.setIsIndividual(true);
			individualGroup.setCreationDate(new Date());
			individualGroup.setId(userGroupDAO.create(individualGroup));
		}
		// note:  we set up multiple nodes and multiple evaluations to ensure that filtering works
		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDAO.createNew(node) );
		};
		if (node2==null) {
			node2 = NodeTestUtils.createNew("bar", Long.parseLong(individualGroup.getId()));
			node2.setId( nodeDAO.createNew(node2) );
		};
		if (evaluation==null) {
			evaluation = createNewEvaluation("foo", individualGroup.getId(), idGenerator, node.getId());
			evaluation.setId( evaluationDAO.create(evaluation, Long.parseLong(individualGroup.getId())) );
		};
	}
	
	public static Evaluation createNewEvaluation(String name, String ownerId, IdGenerator idGenerator, String contentSource) {
		Evaluation evaluation = new Evaluation();
		evaluation.setId(idGenerator.generateNewId().toString());
		evaluation.setContentSource(contentSource);
		evaluation.setOwnerId(ownerId);
		evaluation.setStatus(EvaluationStatus.OPEN);
		evaluation.setName(name);
		evaluation.setCreatedOn(new Date());
		return evaluation;
	}
		
	
	@After
	public void tearDown() throws Exception{
		if (accessRequirement!=null && accessRequirement.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement.getId().toString());
		}
		if (accessRequirement2!=null && accessRequirement2.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement2.getId().toString());
		}
		if (accessRequirement3!=null && accessRequirement3.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement3.getId().toString());
		}
		if (accessRequirement4!=null && accessRequirement4.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement4.getId().toString());
		}
		if (ars!=null) {
			for (AccessRequirement ar : ars) accessRequirementDAO.delete(ar.getId().toString());
			ars.clear();
		}
		if (node!=null && nodeDAO!=null) {
			nodeDAO.delete(node.getId());
			node = null;
		}
		if (node2!=null && nodeDAO!=null) {
			nodeDAO.delete(node2.getId());
			node2 = null;
		}
		if (evaluation!=null && evaluationDAO!=null) {
			evaluationDAO.delete(evaluation.getId());
			evaluation = null;
		}
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup != null) {
			userGroupDAO.delete(individualGroup.getId());
		}
	}
	
	public static TermsOfUseAccessRequirement newEntityAccessRequirement(UserGroup principal, Node node, String text) throws DatastoreException {
		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(principal.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(principal.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod, rod})); // test that repeated IDs doesn't break anything
		accessRequirement.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessRequirements");
		accessRequirement.setTermsOfUse(text);
		return accessRequirement;
	}
	// create an AccessRequirement which restricts both an entity and an Evaluation
	public static TermsOfUseAccessRequirement newMixedAccessRequirement(UserGroup principal, Node node, Evaluation evaluation, String text) throws DatastoreException {
		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(principal.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(principal.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.PARTICIPATE);
		RestrictableObjectDescriptor erod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(evaluation.getId(), RestrictableObjectType.EVALUATION);
		RestrictableObjectDescriptor nrod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{erod, nrod, erod})); // test that repeated IDs doesn't break anything
		accessRequirement.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessRequirements");
		accessRequirement.setTermsOfUse(text);
		return accessRequirement;
	}
	
	// for PLFM-1730 test that if we create a number of access requirements, they come back in the creation order
	@Test
	public void testRetrievalOrder() throws Exception{
		this.ars = new ArrayList<AccessRequirement>();
		for (int i=0; i<10; i++) {
			TermsOfUseAccessRequirement accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo_"+i);			
			ars.add(accessRequirementDAO.create(accessRequirement));
		}
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		List<AccessRequirement> ars2 = accessRequirementDAO.getForSubject(rod);
		assertEquals(ars, ars2);
		
		List<Long> principalIds = new ArrayList<Long>();
		principalIds.add(Long.parseLong(individualGroup.getId()));
		List<Long> arIds = accessRequirementDAO.unmetAccessRequirements(rod, principalIds, ACCESS_TYPE.DOWNLOAD);
		for (int i=0; i<ars.size(); i++) {
			assertEquals(ars.get(i).getId(), arIds.get(i));
		}
	}	
	

	@Test
	public void testEntityAccessRequirementCRUD() throws Exception{
		// Create a new object
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		RestrictableObjectDescriptor subjectId = 
			AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId(), RestrictableObjectType.ENTITY);
		
		testAccessRequirementCRUDIntern(
				accessRequirement,
				subjectId
				);
	}
	
	@Test
	public void testEntityAndEvaluationAccessRequirementCRUD() throws Exception{
		// Create an AccessRequirement that restricts an entity and an Evaluation
		accessRequirement = newMixedAccessRequirement(individualGroup, node, evaluation, "foo");
		RestrictableObjectDescriptor subjectId = 
			AccessRequirementUtilsTest.createRestrictableObjectDescriptor(evaluation.getId(), RestrictableObjectType.EVALUATION);
		
		testAccessRequirementCRUDIntern(
				accessRequirement,
				subjectId
				);
	}
	
		
	private void testAccessRequirementCRUDIntern(AccessRequirement accessRequirement, RestrictableObjectDescriptor subjectId) throws Exception {
		long initialCount = accessRequirementDAO.getCount();
		
		// Create it
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertNotNull(accessRequirement.getId());
		
		assertEquals(1+initialCount, accessRequirementDAO.getCount());
		
		// Fetch it
		// PLFM-1477, we have to check that retrieval works when there is another access requirement
		accessRequirement2 = newEntityAccessRequirement(individualGroup, node2, "bar");
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);		
		AccessRequirement clone = accessRequirementDAO.get(accessRequirement.getId().toString());
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
				
		// Get by Node Id
		Collection<AccessRequirement> ars = accessRequirementDAO.getForSubject(subjectId);
		assertEquals(1, ars.size());
		assertEquals(accessRequirement, ars.iterator().next());

		// update it
		clone = ars.iterator().next();
		clone.setAccessType(ACCESS_TYPE.READ);
		AccessRequirement updatedAR = accessRequirementDAO.update(clone);
		assertEquals(clone.getAccessType(), updatedAR.getAccessType());

		assertTrue("etags should be different after an update", !clone.getEtag().equals(updatedAR.getEtag()));

		try {
			((TermsOfUseAccessRequirement)clone).setTermsOfUse("bar");
			accessRequirementDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e){
			// We expected this exception
		}

		try {
			// Update from a backup.
			updatedAR = accessRequirementDAO.updateFromBackup(clone);
			assertEquals(clone.getEtag(), updatedAR.getEtag());
		}
		catch(ConflictingUpdateException e) {
			fail("Update from backup should not generate exception even if the e-tag is different.");
		}
		// Delete it
		accessRequirementDAO.delete(accessRequirement.getId().toString());
		accessRequirementDAO.delete(accessRequirement2.getId().toString());

		assertEquals(initialCount, accessRequirementDAO.getCount());
	}
}
