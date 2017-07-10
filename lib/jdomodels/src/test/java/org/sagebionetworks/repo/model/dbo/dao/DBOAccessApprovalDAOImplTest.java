package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

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
	
	public static AccessApproval newAccessApproval(UserGroup principal, AccessRequirement ar) throws DatastoreException {
		AccessApproval accessApproval = new AccessApproval();
		accessApproval.setCreatedBy(principal.getId());
		accessApproval.setCreatedOn(new Date());
		accessApproval.setModifiedBy(principal.getId());
		accessApproval.setModifiedOn(new Date());
		accessApproval.setAccessorId(principal.getId());
		accessApproval.setRequirementId(ar.getId());
		accessApproval.setRequirementVersion(ar.getVersionNumber());
		accessApproval.setSubmitterId(principal.getId());
		accessApproval.setState(ApprovalState.APPROVED);
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
		AccessApproval updated = accessApprovalDAO.create(accessApproval);
		accessApproval.setEtag(updated.getEtag());
		assertEquals(accessApproval, updated);

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
		
		List<AccessApproval> ars = accessApprovalDAO.getActiveApprovalsForUser(
				accessRequirement.getId().toString(), individualGroup.getId().toString());
		assertEquals(1, ars.size());
		assertEquals(accessApproval, ars.iterator().next());

		Set<String> requirementIds = accessApprovalDAO.getRequirementsUserHasApprovals(
				individualGroup.getId().toString(), Arrays.asList(accessRequirement.getId().toString(), "-1"));
		assertNotNull(requirementIds);
		assertEquals(1, requirementIds.size());
		assertTrue(requirementIds.contains(accessRequirement.getId().toString()));

		assertTrue(accessApprovalDAO.hasApprovalsSubmittedBy(
				Sets.newHashSet(individualGroup.getId().toString()),
				individualGroup.getId(), accessRequirement.getId().toString()));

		// creating an approval is idempotent:
		// make a second one...
		accessApproval2 = accessApprovalDAO.create(newAccessApproval(individualGroup, accessRequirement));
		ars = accessApprovalDAO.getActiveApprovalsForUser(
				accessRequirement.getId().toString(), individualGroup.getId().toString());
		assertEquals(1, ars.size());
		assertEquals(accessApproval2, ars.get(0));

		// Delete it
		accessApprovalDAO.delete(id);
		assertFalse(accessApprovalDAO.hasApprovalsSubmittedBy(
				Sets.newHashSet(individualGroup.getId().toString()),
				individualGroup.getId(), accessRequirement.getId().toString()));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithNullAccessRequirementId() {
		accessApprovalDAO.revokeAll(null, "1", "2");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithNullAccessorId() {
		accessApprovalDAO.revokeAll("1", null, "2");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testRevokeAccessApprovalsWithNullRevokedBy() {
		accessApprovalDAO.revokeAll("1", "2", null);
	}

	@Test
	public void testRevokeAccessApprovalsWithNotExistingAccessApproval() {
		accessApprovalDAO.revokeAll("-1", "-1", "2");
	}

	@Test
	public void testRevokeAccessApprovalsWithExistingAccessApproval() {
		accessApproval = newAccessApproval(individualGroup, accessRequirement);
		accessApproval = accessApprovalDAO.create(accessApproval);
		accessApprovalDAO.revokeAll(accessRequirement.getId().toString(), individualGroup.getId(), individualGroup2.getId());
		AccessApproval approval = accessApprovalDAO.get(accessApproval.getId().toString());
		assertNotNull(approval);
		assertEquals(ApprovalState.REVOKED, approval.getState());
		assertEquals(individualGroup2.getId(), approval.getModifiedBy());
		assertFalse(accessApproval.getModifiedOn().equals(approval.getModifiedOn()));
		assertFalse(accessApproval.getEtag().equals(approval.getEtag()));
	}

	@Test
	public void testCreateRevokeAndRenewBatch() {
		accessApproval = newAccessApproval(individualGroup, accessRequirement);
		accessApproval2 = newAccessApproval(individualGroup2, accessRequirement);
		accessApprovalDAO.createOrUpdateBatch(Arrays.asList(accessApproval, accessApproval2));

		accessApproval = accessApprovalDAO.getByPrimaryKey(
				accessApproval.getRequirementId(),
				accessApproval.getRequirementVersion(),
				accessApproval.getSubmitterId(),
				accessApproval.getAccessorId());
		accessApproval2 = accessApprovalDAO.getByPrimaryKey(
				accessApproval2.getRequirementId(),
				accessApproval2.getRequirementVersion(),
				accessApproval2.getSubmitterId(),
				accessApproval2.getAccessorId());

		// insert again
		accessApprovalDAO.createOrUpdateBatch(Arrays.asList(accessApproval, accessApproval2));
		AccessApproval updated = accessApprovalDAO.getByPrimaryKey(
				accessApproval.getRequirementId(),
				accessApproval.getRequirementVersion(),
				accessApproval.getSubmitterId(),
				accessApproval.getAccessorId());
		accessApproval.setEtag(updated.getEtag());
		assertEquals(accessApproval, updated);
		AccessApproval updated2 = accessApprovalDAO.getByPrimaryKey(
				accessApproval2.getRequirementId(),
				accessApproval2.getRequirementVersion(),
				accessApproval2.getSubmitterId(),
				accessApproval2.getAccessorId());
		accessApproval2.setEtag(updated2.getEtag());
		assertEquals(accessApproval2, updated2);

		// revoke
		accessApproval.setState(ApprovalState.REVOKED);
		accessApprovalDAO.revokeBySubmitter(
				accessApproval.getRequirementId().toString(),
				accessApproval.getSubmitterId(),
				Arrays.asList(accessApproval.getAccessorId(), accessApproval2.getAccessorId()),
				individualGroup2.getId());
		updated = accessApprovalDAO.getByPrimaryKey(
				accessApproval.getRequirementId(),
				accessApproval.getRequirementVersion(),
				accessApproval.getSubmitterId(),
				accessApproval.getAccessorId());
		assertEquals(ApprovalState.REVOKED, updated.getState());

		// renew
		Date newExpirationDate = new Date();
		Long newVersion = 9L;
		accessApproval.setExpiredOn(newExpirationDate);
		accessApproval.setRequirementVersion(newVersion);
		accessApprovalDAO.createOrUpdateBatch(Arrays.asList(accessApproval));
		updated = accessApprovalDAO.getByPrimaryKey(
				accessApproval.getRequirementId(),
				newVersion,
				accessApproval.getSubmitterId(),
				accessApproval.getAccessorId());
		assertEquals(newVersion, updated.getRequirementVersion());
		assertEquals(newExpirationDate, updated.getExpiredOn());

		// clean up
		accessApprovalDAO.delete(accessApproval.getId().toString());
		accessApprovalDAO.delete(accessApproval2.getId().toString());
	}

	@Test
	public void testListAccessorListAndRevokeGroup() {
		List<AccessorGroup> result = accessApprovalDAO.listAccessorGroup(accessRequirement.getId().toString(),
				individualGroup.getId(), null, 10L, 0L);
		assertNotNull(result);
		assertTrue(result.isEmpty());

		// create some approvals
		accessApproval = newAccessApproval(individualGroup, accessRequirement);
		accessApproval2 = newAccessApproval(individualGroup2, accessRequirement);
		accessApproval2.setSubmitterId(individualGroup.getId());
		accessApprovalDAO.createOrUpdateBatch(Arrays.asList(accessApproval, accessApproval2));
		result = accessApprovalDAO.listAccessorGroup(accessRequirement.getId().toString(),
				individualGroup.getId(), null, 10L, 0L);
		assertNotNull(result);
		assertEquals(1, result.size());
		AccessorGroup group = result.get(0);
		assertNotNull(group);
		assertEquals(accessRequirement.getId().toString(), group.getAccessRequirementId());
		assertEquals(individualGroup.getId(), group.getSubmitterId());
		assertTrue(group.getAccessorIds().contains(individualGroup.getId()));
		assertTrue(group.getAccessorIds().contains(individualGroup2.getId()));
		assertEquals(new Date(DBOAccessApprovalDAOImpl.DEFAULT_NOT_EXPIRED), group.getExpiredOn());

		// revoke the group
		accessApprovalDAO.revokeGroup(accessRequirement.getId().toString(), individualGroup.getId(), individualGroup2.getId());
		result = accessApprovalDAO.listAccessorGroup(accessRequirement.getId().toString(),
				individualGroup.getId(), null, 10L, 0L);
		assertNotNull(result);
		assertTrue(result.isEmpty());

		// check each approval
		AccessApproval approval = accessApprovalDAO.getByPrimaryKey(accessRequirement.getId(),
				accessRequirement.getVersionNumber(), individualGroup.getId(), individualGroup.getId());
		assertNotNull(approval);
		assertEquals(ApprovalState.REVOKED, approval.getState());
		assertEquals(individualGroup2.getId(), approval.getModifiedBy());

		AccessApproval approval2 = accessApprovalDAO.getByPrimaryKey(accessRequirement.getId(),
				accessRequirement.getVersionNumber(), individualGroup.getId(), individualGroup2.getId());
		assertNotNull(approval2);
		assertEquals(ApprovalState.REVOKED, approval2.getState());
		assertEquals(individualGroup2.getId(), approval2.getModifiedBy());
	}

	@Test
	public void testConvertToList() {
		assertEquals(new LinkedList<String>(), DBOAccessApprovalDAOImpl.convertToList(null));
		assertEquals(Arrays.asList("1"), DBOAccessApprovalDAOImpl.convertToList("1"));
		assertEquals(Arrays.asList("1","2"), DBOAccessApprovalDAOImpl.convertToList("1,2"));
	}

	@Test
	public void testBuildQuery() {
		assertEquals("SELECT REQUIREMENT_ID, SUBMITTER_ID, EXPIRED_ON, GROUP_CONCAT(DISTINCT ACCESSOR_ID SEPARATOR ',') AS ACCESSOR_LIST"
				+ " FROM ACCESS_APPROVAL"
				+ " WHERE STATE = 'APPROVED'"
				+ " GROUP BY REQUIREMENT_ID, SUBMITTER_ID, EXPIRED_ON"
				+ " ORDER BY EXPIRED_ON"
				+ " LIMIT :LIMIT"
				+ " OFFSET :OFFSET",
				DBOAccessApprovalDAOImpl.buildAccessorGroupQuery(null, null, null));
		assertEquals("SELECT REQUIREMENT_ID, SUBMITTER_ID, EXPIRED_ON, GROUP_CONCAT(DISTINCT ACCESSOR_ID SEPARATOR ',') AS ACCESSOR_LIST"
				+ " FROM ACCESS_APPROVAL"
				+ " WHERE STATE = 'APPROVED'"
				+ " AND REQUIREMENT_ID = :REQUIREMENT_ID"
				+ " GROUP BY REQUIREMENT_ID, SUBMITTER_ID, EXPIRED_ON"
				+ " ORDER BY EXPIRED_ON"
				+ " LIMIT :LIMIT"
				+ " OFFSET :OFFSET",
				DBOAccessApprovalDAOImpl.buildAccessorGroupQuery("1", null, null));
		assertEquals("SELECT REQUIREMENT_ID, SUBMITTER_ID, EXPIRED_ON, GROUP_CONCAT(DISTINCT ACCESSOR_ID SEPARATOR ',') AS ACCESSOR_LIST"
				+ " FROM ACCESS_APPROVAL"
				+ " WHERE STATE = 'APPROVED'"
				+ " AND SUBMITTER_ID = :SUBMITTER_ID"
				+ " GROUP BY REQUIREMENT_ID, SUBMITTER_ID, EXPIRED_ON"
				+ " ORDER BY EXPIRED_ON"
				+ " LIMIT :LIMIT"
				+ " OFFSET :OFFSET",
				DBOAccessApprovalDAOImpl.buildAccessorGroupQuery(null, "2", null));
		assertEquals("SELECT REQUIREMENT_ID, SUBMITTER_ID, EXPIRED_ON, GROUP_CONCAT(DISTINCT ACCESSOR_ID SEPARATOR ',') AS ACCESSOR_LIST"
				+ " FROM ACCESS_APPROVAL"
				+ " WHERE STATE = 'APPROVED'"
				+ " AND EXPIRED_ON <> 0"
				+ " AND EXPIRED_ON <= :EXPIRED_ON"
				+ " GROUP BY REQUIREMENT_ID, SUBMITTER_ID, EXPIRED_ON"
				+ " ORDER BY EXPIRED_ON"
				+ " LIMIT :LIMIT"
				+ " OFFSET :OFFSET",
				DBOAccessApprovalDAOImpl.buildAccessorGroupQuery(null, null, new Date()));
	}
}
