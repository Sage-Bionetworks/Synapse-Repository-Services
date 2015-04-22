package org.sagebionetworks.acl.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.dao.AclRecordDAO;
import org.sagebionetworks.audit.dao.ResourceAccessRecordDAO;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.audit.ResourceAccessRecord;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class AclSnapshotWorkerIntegrationTest {

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	StackConfiguration config;
	@Autowired
	private AclRecordDAO aclRecordDao;
	@Autowired
	private ResourceAccessRecordDAO resourceAccessRecordDao;
	
	@Autowired
	private AccessControlListDAO aclDao;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private UserGroupDAO userGroupDao;
	
	@Autowired
	private AmazonS3Client s3Client;

	private Node node;
	private UserGroup group;
	private UserGroup group2;
	
	private Collection<UserGroup> groupList = new ArrayList<UserGroup>();
	private Collection<AccessControlList> aclList = new ArrayList<AccessControlList>();
	
	private Long createdById;
	private Long modifiedById;
	
	private static final int TIME_OUT = 60 * 1000;
	
	@Before
	public void before() throws Exception {
		assertNotNull(idGenerator);
		assertNotNull(config);
		assertNotNull(aclRecordDao);
		assertNotNull(resourceAccessRecordDao);

		assertNotNull(aclDao);
		assertNotNull(nodeDao);
		assertNotNull(userGroupDao);

		assertNotNull(s3Client);

		createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

		// strictly speaking it's nonsensical for a group to be a 'modifier'.  we're just using it for testing purposes
		modifiedById = BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();

		// create a resource on which to apply permissions
		node = new Node();
		node.setName("testNodeForAclSnapshotWorker");
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(createdById);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(modifiedById);
		node.setNodeType(EntityType.project);
		String nodeId = nodeDao.createNew(node);
		assertNotNull(nodeId);
		node = nodeDao.getNode(nodeId);

		// create a group to give the permissions to
		group = new UserGroup();
		group.setIsIndividual(false);
		group.setId(userGroupDao.create(group).toString());
		assertNotNull(group.getId());
		groupList.add(group);

		// Create a second user
		group2 = new UserGroup();
		group2.setIsIndividual(false);
		group2.setId(userGroupDao.create(group2).toString());
		assertNotNull(group2.getId());
		groupList.add(group2);
	}

	@Test
	public void testCreate() throws Exception {
		Set<String> aclKeys = aclRecordDao.listAllKeys();
		Set<String> resourceAccessKeys = resourceAccessRecordDao.listAllKeys();
		assertNotNull(aclKeys);
		assertNotNull(resourceAccessKeys);

		// Prepare the acl to create
		AccessControlList acl = new AccessControlList();
		acl.setId(node.getId());
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		aclList.add(acl);
		Set<ResourceAccess> ras =
				AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(
						Long.parseLong(group.getId()), Long.parseLong(group2.getId())), 2, false);
		acl.setResourceAccess(ras);
		// create the acl
		String aclId = aclDao.create(acl, ObjectType.ENTITY);
		assertEquals(node.getId(), aclId);

		// build the expecting aclRecord and raRecords
		AclRecord expectedAclRecord = new AclRecord();
		expectedAclRecord.setAclId(aclDao.getAclId(node.getId(), ObjectType.ENTITY).toString());
		expectedAclRecord.setChangeType(ChangeType.CREATE);
		expectedAclRecord.setCreationDate(acl.getCreationDate());
		expectedAclRecord.setEtag(acl.getEtag());
		expectedAclRecord.setOwnerId(node.getId());
		expectedAclRecord.setOwnerType(ObjectType.ENTITY);

		Set<ResourceAccessRecord> expectedRaRecords = AclSnapshotWorkerTestUtils.createSetOfResourceAccessRecord(ras);

		assertTrue(waitForObjects(aclKeys, resourceAccessKeys, expectedAclRecord, expectedRaRecords));
	}

	@Test
	public void testUpdate() throws Exception {
		// Prepare the acl to create
		AccessControlList acl = new AccessControlList();
		acl.setId(node.getId());
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		aclList.add(acl);
		Set<ResourceAccess> ras =
				AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(
						Long.parseLong(group.getId()), Long.parseLong(group2.getId())), 2, false);
		acl.setResourceAccess(ras);
		// create the acl
		String aclId = aclDao.create(acl, ObjectType.ENTITY);
		assertEquals(node.getId(), aclId);

		Set<String> aclKeys = aclRecordDao.listAllKeys();
		Set<String> resourceAccessKeys = resourceAccessRecordDao.listAllKeys();
		assertNotNull(aclKeys);
		assertNotNull(resourceAccessKeys);

		// prepare the ACL to update
		acl = aclDao.get(node.getId(), ObjectType.ENTITY);
		assertNotNull(acl);
		assertNotNull(acl.getEtag());
		assertEquals(node.getId(), acl.getId());
		ras = AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(
				Long.parseLong(group.getId()), Long.parseLong(group2.getId())), 2, true);
		acl.setResourceAccess(ras);

		// update the ACL
		aclDao.update(acl, ObjectType.ENTITY);

		// build the expecting aclRecord and raRecords
		AclRecord expectedAclRecord = new AclRecord();
		expectedAclRecord.setAclId(aclDao.getAclId(node.getId(), ObjectType.ENTITY).toString());
		expectedAclRecord.setChangeType(ChangeType.UPDATE);
		expectedAclRecord.setCreationDate(acl.getCreationDate());
		expectedAclRecord.setEtag(acl.getEtag());
		expectedAclRecord.setOwnerId(node.getId());
		expectedAclRecord.setOwnerType(ObjectType.ENTITY);

		Set<ResourceAccessRecord> expectedRaRecords = AclSnapshotWorkerTestUtils.createSetOfResourceAccessRecord(ras);
		assertTrue(waitForObjects(aclKeys, resourceAccessKeys, expectedAclRecord, expectedRaRecords));

	}

	@Test
	public void testDelete() throws Exception {
		// Prepare the acl to create
		AccessControlList acl = new AccessControlList();
		acl.setId(node.getId());
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		aclList.add(acl);
		Set<ResourceAccess> ras =
				AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(
						Long.parseLong(group.getId()), Long.parseLong(group2.getId())), 2, false);
		acl.setResourceAccess(ras);
		// create the acl
		String aclId = aclDao.create(acl, ObjectType.ENTITY);
		assertEquals(node.getId(), aclId);

		Set<String> aclKeys = aclRecordDao.listAllKeys();
		Set<String> resourceAccessKeys = resourceAccessRecordDao.listAllKeys();
		assertNotNull(aclKeys);
		assertNotNull(resourceAccessKeys);

		// build the expecting aclRecord before deleting the ACL
		AclRecord expectedAclRecord = new AclRecord();
		expectedAclRecord.setAclId(aclDao.getAclId(node.getId(), ObjectType.ENTITY).toString());
		expectedAclRecord.setChangeType(ChangeType.DELETE);

		// Delete the acl
		aclDao.delete(node.getId(), ObjectType.ENTITY);

		assertTrue(waitForObjects(aclKeys, resourceAccessKeys, expectedAclRecord, null));
	}

	@After 
	public void cleanUp() throws Exception {
		nodeDao.delete(node.getId());
		for (UserGroup g : groupList) {
			userGroupDao.delete(g.getId());
		}
		for (AccessControlList acl : aclList) {
			aclDao.delete(acl.getId(), ObjectType.ENTITY);
		}
		groupList.clear();
		aclRecordDao.deleteAllStackInstanceBatches();
		resourceAccessRecordDao.deleteAllStackInstanceBatches();
	}

	/**
	 * Helper method that keep looking for new AclRecord log files and new 
	 * ResourceAccessRecord log files in S3.
	 * 
	 * @return true if found the expectedAclRecord and expectedRaRecords in TIME_OUT milliseconds,
	 *         false otherwise.
	 */
	private boolean waitForObjects(Set<String> oldAclKeys, Set<String> oldResourceAccessKeys,
			AclRecord expectedAclRecord, Set<ResourceAccessRecord> expectedRaRecords) throws Exception {
		long start = System.currentTimeMillis();
		boolean foundAclRecord = false;

		while (System.currentTimeMillis() < start + TIME_OUT) {
			Set<String> newAclKeys = null;
			if (!foundAclRecord) {
				newAclKeys = aclRecordDao.listAllKeys();
				newAclKeys.removeAll(oldAclKeys);
			}

			if (!foundAclRecord && newAclKeys.size() != 0) {
				foundAclRecord = findAclRecord(expectedAclRecord, newAclKeys);
			}

			if (foundAclRecord && expectedRaRecords == null) {
				return true;
			}

			Set<String> newResourceKeys = null;
			if (foundAclRecord) {
				newResourceKeys = resourceAccessRecordDao.listAllKeys();
				newResourceKeys.removeAll(oldResourceAccessKeys);
			}

			if (foundAclRecord && newResourceKeys.size() != 0) {
				if (findRaRecords(expectedRaRecords, newResourceKeys)) {
					return true;
				}
			}

			// wait for 1 second before calling the service again
			Thread.sleep(1000);
		}
		return false;
	}

	/**
	 * @param expectedRaRecords
	 * @param newResourceKeys
	 * @return true if the newResourceKeys contains expectedRaRecords 
	 * @throws IOException
	 */
	private boolean findRaRecords(Set<ResourceAccessRecord> expectedRaRecords, Set<String> newResourceKeys) throws IOException {
		// newResourceKeys should have size 1; otherwise, we are going over some old keys
		for (String raKey : newResourceKeys) {
			List<ResourceAccessRecord> newRaRecords = resourceAccessRecordDao.getBatch(raKey);

			if (compareRaRecords(new HashSet<ResourceAccessRecord>(newRaRecords), expectedRaRecords)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param expectedAclRecord
	 * @param newAclKeys
	 * @return true if the newAclKeys contains expectedAclRecord
	 * @throws IOException
	 */
	private boolean findAclRecord(AclRecord expectedAclRecord, Set<String> newAclKeys) throws IOException {
		// newAclKeys should have size 1; otherwise, we are going over some old keys
		for (String aclKey : newAclKeys) {
			List<AclRecord> aclRecords = aclRecordDao.getBatch(aclKey);
			assertEquals(1, aclRecords.size());
			AclRecord newAclRecord = aclRecords.get(0);

			if (compareAclRecords(newAclRecord, expectedAclRecord)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Since there are information that we do not know when constructing the
	 * expected records, we only check the fields that we know.
	 * @param newRaRecords
	 * @param expectedRaRecords
	 * @return true if newRaRecords and expectedRaRecords contains the same set
	 * of records, ignoring the chamgeNumber field,
	 *         false otherwise.
	 */
	private boolean compareRaRecords(HashSet<ResourceAccessRecord> newRaRecords,
			Set<ResourceAccessRecord> expectedRaRecords) {
		if (newRaRecords.size() != expectedRaRecords.size()) {
			return false;
		}
		for (ResourceAccessRecord newRaRecord : newRaRecords) {
			newRaRecord.setChangeNumber(null);
			if (!expectedRaRecords.contains(newRaRecord)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Since there are information that we do not know when constructing the
	 * expected records, we only check the fields that we know.
	 */
	private boolean compareAclRecords(AclRecord newAclRecord,
			AclRecord expectedAclRecord) {
		return (newAclRecord.getAclId().equals(expectedAclRecord.getAclId()) &&
				newAclRecord.getChangeType().equals(ChangeType.DELETE) &&
				expectedAclRecord.getChangeType().equals(ChangeType.DELETE))
				||
				(newAclRecord.getAclId().equals(expectedAclRecord.getAclId()) &&
				newAclRecord.getChangeType().equals(expectedAclRecord.getChangeType()) &&
				(Math.abs(newAclRecord.getCreationDate().getTime() - expectedAclRecord.getCreationDate().getTime()) < 1000) &&
				newAclRecord.getEtag().equals(expectedAclRecord.getEtag()) &&
				newAclRecord.getOwnerId().equals(expectedAclRecord.getOwnerId()) &&
				newAclRecord.getOwnerType().equals(expectedAclRecord.getOwnerType()));
	}
}
