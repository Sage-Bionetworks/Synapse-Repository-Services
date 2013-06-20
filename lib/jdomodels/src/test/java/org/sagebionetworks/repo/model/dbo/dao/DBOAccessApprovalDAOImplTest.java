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
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessApprovalDAOImplTest {

	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	AccessRequirementDAO accessRequirementDAO;
		
	@Autowired
	AccessApprovalDAO accessApprovalDAO;
		
	@Autowired
	NodeDAO nodeDAO;
	
	@Autowired
	EvaluationDAO evaluationDAO;
	
	@Autowired
	private IdGenerator idGenerator;

	private static final String TEST_USER_NAME = "test-user";
	private static final String TEST_USER_NAME_2 = "test-user-2";
	
	private UserGroup individualGroup = null;
	private UserGroup individualGroup2 = null;
	private Node node = null;
	private Node node2 = null;
	private AccessRequirement accessRequirement = null;
	private AccessRequirement accessRequirement2 = null;
	private AccessApproval accessApproval = null;
	private AccessApproval accessApproval2 = null;
	private Evaluation evaluation = null;
	private List<ACCESS_TYPE> participateAndDownload=null;
	private List<ACCESS_TYPE> downloadAccessType=null;
	private List<ACCESS_TYPE> updateAccessType=null;
	
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
		individualGroup2 = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup2 == null) {
			individualGroup2 = new UserGroup();
			individualGroup2.setName(TEST_USER_NAME_2);
			individualGroup2.setIsIndividual(true);
			individualGroup2.setCreationDate(new Date());
			individualGroup2.setId(userGroupDAO.create(individualGroup2));
		}
		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDAO.createNew(node) );
		};
		if (node2==null) {
			node2 = NodeTestUtils.createNew("bar", Long.parseLong(individualGroup.getId()));
			node2.setId( nodeDAO.createNew(node2) );
		};
		accessRequirement = DBOAccessRequirementDAOImplTest.newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		Long id = accessRequirement.getId();
		assertNotNull(id);
		
		if (evaluation==null) {
			evaluation = DBOAccessRequirementDAOImplTest.createNewEvaluation("foo", individualGroup.getId(), idGenerator, node.getId());
			evaluation.setId( evaluationDAO.create(evaluation, Long.parseLong(individualGroup.getId())) );
		};
		accessRequirement2 = DBOAccessRequirementDAOImplTest.newMixedAccessRequirement(individualGroup, node2, evaluation, "bar");
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);
		id = accessRequirement2.getId();
		assertNotNull(id);

		if (participateAndDownload == null) {
			participateAndDownload = new ArrayList<ACCESS_TYPE>();
			participateAndDownload.add(ACCESS_TYPE.DOWNLOAD);
			participateAndDownload.add(ACCESS_TYPE.PARTICIPATE);
		}
		
		if (downloadAccessType == null) {
			downloadAccessType= new ArrayList<ACCESS_TYPE>();
			downloadAccessType.add(ACCESS_TYPE.DOWNLOAD);
		}
		if (updateAccessType == null) {
			updateAccessType= new ArrayList<ACCESS_TYPE>();
			updateAccessType.add(ACCESS_TYPE.UPDATE);
		}
}
		
	
	@After
	public void tearDown() throws Exception{
		if (accessApproval!=null && accessApproval.getId()!=null) {
			accessApprovalDAO.delete(accessApproval.getId().toString());
		}
		if (accessApproval2!=null && accessApproval2.getId()!=null) {
			accessApprovalDAO.delete(accessApproval2.getId().toString());
		}
		if (accessRequirement!=null && accessRequirement.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement.getId().toString());
		}
		if (accessRequirement2!=null && accessRequirement2.getId()!=null) {
			accessRequirementDAO.delete(accessRequirement2.getId().toString());
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
		individualGroup2 = userGroupDAO.findGroup(TEST_USER_NAME_2, true);
		if (individualGroup2 != null) {
			userGroupDAO.delete(individualGroup2.getId());
		}
	}
	
	public static TermsOfUseAccessApproval newAccessApproval(UserGroup principal, AccessRequirement ar) throws DatastoreException {
		TermsOfUseAccessApproval accessApproval = new TermsOfUseAccessApproval();
		accessApproval.setCreatedBy(principal.getId());
		accessApproval.setCreatedOn(new Date());
		accessApproval.setModifiedBy(principal.getId());
		accessApproval.setModifiedOn(new Date());
		accessApproval.setEtag("10");
		accessApproval.setAccessorId(principal.getId());
		accessApproval.setRequirementId(ar.getId());
		accessApproval.setEntityType("com.sagebionetworks.repo.model.TermsOfUseAccessApproval");
		return accessApproval;
	}
	
	@Test
	public void testUnmetARsForEvaluation() throws Exception {
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(evaluation.getId(), RestrictableObjectType.EVALUATION);
		// Logic for unmet requirements doesn't reflect ownership at the DAO level.  It's factored in at the manager level.
		// Therefore, the owner see unmet ARs for herself..
		List<Long> unmetARIds = accessRequirementDAO.unmetAccessRequirements(rod, Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), participateAndDownload);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement2.getId(), unmetARIds.iterator().next());
		// ... just as someone else does, if they haven't signed the ToU
		unmetARIds = accessRequirementDAO.unmetAccessRequirements(rod, Arrays.asList(new Long[]{Long.parseLong(individualGroup2.getId())}), participateAndDownload);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement2.getId(), unmetARIds.iterator().next());
		
		
		// Create a new object
		accessApproval2 = newAccessApproval(individualGroup2, accessRequirement2);
		
		// Create it
		accessApproval2 = accessApprovalDAO.create(accessApproval2);
		String id = accessApproval2.getId().toString();
		assertNotNull(id);
		
		// no unmet requirement anymore ...
		assertTrue(
				accessRequirementDAO.unmetAccessRequirements(
						rod, 
						Arrays.asList(new Long[]{Long.parseLong(individualGroup2.getId())}), 
						participateAndDownload).isEmpty()
				);

		// Get by evaluation Id
		Collection<AccessApproval> ars = accessApprovalDAO.getForAccessRequirementsAndPrincipals(
				Arrays.asList(new String[]{accessRequirement2.getId().toString()}), 
				Arrays.asList(new String[]{individualGroup2.getId().toString()}));
		assertEquals(1, ars.size());
		assertEquals(accessApproval2, ars.iterator().next());
	}
	
	@Test
	public void testCRUD() throws Exception {
		// first of all, we should see the unmet requirement
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		List<Long> unmetARIds = accessRequirementDAO.unmetAccessRequirements(rod, Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), downloadAccessType);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement.getId(), unmetARIds.iterator().next());
		// while we're at it, check the edge cases:
		// same result for ficticious principal ID
		unmetARIds = accessRequirementDAO.unmetAccessRequirements(rod, Arrays.asList(new Long[]{8888L}), downloadAccessType);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement.getId(), unmetARIds.iterator().next());
		// no unmet requirements for ficticious node ID
		assertTrue(
				accessRequirementDAO.unmetAccessRequirements(
						AccessRequirementUtilsTest.createRestrictableObjectDescriptor("syn7890"), 
						Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), 
						downloadAccessType).isEmpty()
				);
		// no unmet requirement for other type of access
		assertTrue(
				accessRequirementDAO.unmetAccessRequirements(
						rod, 
						Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), 
						updateAccessType).isEmpty()
				);
		
		
		// Create a new object
		accessApproval = newAccessApproval(individualGroup, accessRequirement);
		
		// Create it
		accessApproval = accessApprovalDAO.create(accessApproval);
		String id = accessApproval.getId().toString();
		assertNotNull(id);
		
		// no unmet requirement anymore ...
		assertTrue(
				accessRequirementDAO.unmetAccessRequirements(
						rod, 
						Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), 
						downloadAccessType).isEmpty()
				);
		// ... but for a different (ficticious) user, the requirement isn't met...
		unmetARIds = accessRequirementDAO.unmetAccessRequirements(rod, Arrays.asList(new Long[]{8888L}), downloadAccessType);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement.getId(), unmetARIds.iterator().next());
		// ... and it's still unmet for the second node
		RestrictableObjectDescriptor rod2 = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node2.getId());
		unmetARIds = accessRequirementDAO.unmetAccessRequirements(rod2, Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), participateAndDownload);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement2.getId(), unmetARIds.iterator().next());
		
		// Fetch it
		AccessApproval clone = accessApprovalDAO.get(id);
		assertNotNull(clone);
		assertEquals(accessApproval, clone);
		
		// Get by Node Id
		Collection<AccessApproval> ars = accessApprovalDAO.getForAccessRequirementsAndPrincipals(
				Arrays.asList(new String[]{accessRequirement.getId().toString()}), 
				Arrays.asList(new String[]{individualGroup.getId().toString()}));
		assertEquals(1, ars.size());
		assertEquals(accessApproval, ars.iterator().next());

		// update it
		clone = ars.iterator().next();
		AccessApproval updatedAA = accessApprovalDAO.update(clone);
		assertEquals(((TermsOfUseAccessApproval)clone).getEntityType(), ((TermsOfUseAccessApproval)updatedAA).getEntityType());
		assertTrue("etags should be different after an update", !clone.getEtag().equals(updatedAA.getEtag()));

		try {
			accessApprovalDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e){
			// We expected this exception
		}

		try {
			// Update from a backup.
			updatedAA = accessApprovalDAO.updateFromBackup(clone);
			assertEquals(clone.getEtag(), updatedAA.getEtag());
		}
		catch(ConflictingUpdateException e) {
			fail("Update from backup should not generate exception even if the e-tag is different.");
		}

		// Delete it
		accessApprovalDAO.delete(id);
	}
}
