package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
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
	NodeDAO nodeDao;
	
	private UserGroup individualGroup = null;
	private UserGroup individualGroup2 = null;
	private Node node = null;
	private Node node2 = null;
	private AccessRequirement accessRequirement = null;
	private AccessRequirement accessRequirement2 = null;
	private AccessApproval accessApproval = null;
	private AccessApproval accessApproval2 = null;
	private List<ACCESS_TYPE> participateAndDownload=null;
	private List<ACCESS_TYPE> downloadAccessType=null;
	private List<ACCESS_TYPE> updateAccessType=null;
	
	@Before
	public void setUp() throws Exception {

		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());

		individualGroup2 = new UserGroup();
		individualGroup2.setIsIndividual(true);
		individualGroup2.setCreationDate(new Date());
		individualGroup2.setId(userGroupDAO.create(individualGroup2).toString());

		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDao.createNew(node) );
		};
		if (node2==null) {
			node2 = NodeTestUtils.createNew("bar", Long.parseLong(individualGroup.getId()));
			node2.setId( nodeDao.createNew(node2) );
		};
		accessRequirement = DBOAccessRequirementDAOImplTest.newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		Long id = accessRequirement.getId();
		assertNotNull(id);
		accessRequirement2 = DBOAccessRequirementDAOImplTest.newEntityAccessRequirement(individualGroup, node2, "bar");
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
		if (node!=null && nodeDao!=null) {
			nodeDao.delete(node.getId());
			node = null;
		}
		if (node2!=null && nodeDao!=null) {
			nodeDao.delete(node2.getId());
			node2 = null;
		}
		if (individualGroup != null) {
			userGroupDAO.delete(individualGroup.getId());
		}
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
		accessApproval.setAccessorId(principal.getId());
		accessApproval.setRequirementId(ar.getId());
		accessApproval.setConcreteType("com.sagebionetworks.repo.model.TermsOfUseAccessApproval");
		return accessApproval;
	}
	
	@Test
	public void testCRUD() throws Exception {
		// first of all, we should see the unmet requirement
		List<Long> unmetARIds = accessRequirementDAO.getAllUnmetAccessRequirements(Collections.singletonList(node.getId()), RestrictableObjectType.ENTITY, 
				Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), downloadAccessType);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement.getId(), unmetARIds.iterator().next());
		// while we're at it, check the edge cases:
		// same result for ficticious principal ID
		unmetARIds = accessRequirementDAO.getAllUnmetAccessRequirements(Collections.singletonList(node.getId()), RestrictableObjectType.ENTITY, 
				Arrays.asList(new Long[]{8888L}), downloadAccessType);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement.getId(), unmetARIds.iterator().next());
		Set<String> arSet = new HashSet<String>();
		arSet.add(accessRequirement.getId().toString());
		assertTrue(accessApprovalDAO.hasUnmetAccessRequirement(arSet, individualGroup.getId()));
		// no unmet requirements for ficticious node ID
		assertTrue(
				accessRequirementDAO.getAllUnmetAccessRequirements(
						Collections.singletonList("syn7890"), RestrictableObjectType.ENTITY, 
						Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), 
						downloadAccessType).isEmpty()
				);
		// no unmet requirement for other type of access
		assertTrue(
				accessRequirementDAO.getAllUnmetAccessRequirements(
						Collections.singletonList(node.getId()), RestrictableObjectType.ENTITY,
						Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), 
						updateAccessType).isEmpty()
				);

		List<AccessApproval> approvals = accessApprovalDAO.getAccessApprovalsForSubjects(
				Arrays.asList(node.getId()), RestrictableObjectType.ENTITY, 10L, 0L);
		assertNotNull(approvals);
		assertTrue(approvals.isEmpty());

		// Create a new object
		accessApproval = newAccessApproval(individualGroup, accessRequirement);
		
		// Create it
		accessApproval = accessApprovalDAO.create(accessApproval);
		String id = accessApproval.getId().toString();
		assertNotNull(id);
		assertNotNull(accessApproval.getEtag());

		// test create again
		assertEquals(accessApproval, accessApprovalDAO.create(accessApproval));

		approvals = accessApprovalDAO.getAccessApprovalsForSubjects(
				Arrays.asList(node.getId()), RestrictableObjectType.ENTITY, 10L, 0L);
		assertNotNull(approvals);
		assertEquals(1, approvals.size());
		assertEquals(accessApproval, approvals.get(0));

		// no unmet requirement anymore ...
		assertTrue(
				accessRequirementDAO.getAllUnmetAccessRequirements(
						Collections.singletonList(node.getId()), RestrictableObjectType.ENTITY, 
						Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), 
						downloadAccessType).isEmpty()
				);
		assertFalse(accessApprovalDAO.hasUnmetAccessRequirement(arSet, individualGroup.getId()));
		
		// ... but for a different (ficticious) user, the requirement isn't met...
		unmetARIds = accessRequirementDAO.getAllUnmetAccessRequirements(Collections.singletonList(node.getId()), RestrictableObjectType.ENTITY, 
				Arrays.asList(new Long[]{8888L}), downloadAccessType);
		assertEquals(1, unmetARIds.size());
		assertEquals(accessRequirement.getId(), unmetARIds.iterator().next());
		// ... and it's still unmet for the second node
		unmetARIds = accessRequirementDAO.getAllUnmetAccessRequirements(Collections.singletonList(node2.getId()), RestrictableObjectType.ENTITY,
				Arrays.asList(new Long[]{Long.parseLong(individualGroup.getId())}), participateAndDownload);
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

		Set<String> approvedUsers = accessApprovalDAO.getApprovedUsers(Arrays.asList(individualGroup.getId().toString()), accessRequirement.getId().toString());
		assertNotNull(approvedUsers);
		assertEquals(1, approvedUsers.size());
		assertTrue(approvedUsers.contains(individualGroup.getId().toString()));

		// update it
		clone = ars.iterator().next();
		AccessApproval updatedAA = accessApprovalDAO.update(clone);
		assertEquals(((TermsOfUseAccessApproval)clone).getConcreteType(), ((TermsOfUseAccessApproval)updatedAA).getConcreteType());
		assertTrue("etags should be different after an update", !clone.getEtag().equals(updatedAA.getEtag()));

		try {
			accessApprovalDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e){
			// We expected this exception
		}
		
		// creating an approval is idempotent:
		// make a second one...
		accessApproval2 = accessApprovalDAO.create(newAccessApproval(individualGroup, accessRequirement));
		assertEquals(updatedAA, accessApproval2);
		// .. there is still only one:
		ars = accessApprovalDAO.getForAccessRequirementsAndPrincipals(
				Arrays.asList(new String[]{accessRequirement.getId().toString()}), 
				Arrays.asList(new String[]{individualGroup.getId().toString()}));
		assertEquals(1, ars.size());

		// Delete it
		accessApprovalDAO.delete(id);
		approvedUsers = accessApprovalDAO.getApprovedUsers(Arrays.asList(individualGroup.getId().toString()), accessRequirement.getId().toString());
		assertNotNull(approvedUsers);
		assertTrue(approvedUsers.isEmpty());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteAccessApprovalWithNullAccessRequirementId() {
		accessApprovalDAO.delete(null, "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteAccessApprovalWithNullAccessorId() {
		accessApprovalDAO.delete("1", null);
	}

	@Test
	public void testDeleteAccessApprovalWithNotExistingAccessApproval() {
		accessApprovalDAO.delete("-1", "-1");
	}

	@Test
	public void testDeleteAccessApprovalWithExistingAccessApproval() {
		accessApproval = newAccessApproval(individualGroup, accessRequirement);
		accessApproval = accessApprovalDAO.create(accessApproval);
		accessApprovalDAO.delete(accessRequirement.getId().toString(), individualGroup.getId());
		try {
			accessApprovalDAO.get(accessApproval.getId().toString());
			fail("Expecting a NotFoundException");
		} catch (NotFoundException e) {
			// make sure that the exception is thrown here and not before this call
		}
	}

	@Test
	public void testCreateAndDeleteBatch() {
		assertTrue(accessApprovalDAO.getForAccessRequirement(accessRequirement.getId().toString()).isEmpty());
		accessApproval = newAccessApproval(individualGroup, accessRequirement);
		accessApproval2 = newAccessApproval(individualGroup2, accessRequirement);
		List<AccessApproval> created = accessApprovalDAO.createBatch(Arrays.asList(accessApproval, accessApproval2));
		assertEquals(2, accessApprovalDAO.getForAccessRequirement(accessRequirement.getId().toString()).size());

		// insert again
		assertEquals(created, accessApprovalDAO.createBatch(Arrays.asList(accessApproval, accessApproval2)));

		List<Long> toDelete = new LinkedList<Long>();
		toDelete.add(created.get(0).getId());
		toDelete.add(created.get(1).getId());

		assertEquals(2, accessApprovalDAO.deleteBatch(toDelete));
		assertTrue(accessApprovalDAO.getForAccessRequirement(accessRequirement.getId().toString()).isEmpty());
	}
}
