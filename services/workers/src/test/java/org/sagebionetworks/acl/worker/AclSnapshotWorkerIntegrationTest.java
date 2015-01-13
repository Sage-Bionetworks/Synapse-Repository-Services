package org.sagebionetworks.acl.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.dao.AclRecordDAO;
import org.sagebionetworks.audit.dao.ResourceAccessRecordDAO;
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

// it's necessary to drop the database every time before running this test
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class AclSnapshotWorkerIntegrationTest {

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
	
	private int TIME_OUT = 60 * 1000;
	
	@Before
	public void before() throws Exception {
		assertNotNull(config);
		assertNotNull(aclRecordDao);
		assertNotNull(resourceAccessRecordDao);

		assertNotNull(aclDao);
		assertNotNull(nodeDao);
		assertNotNull(userGroupDao);

		assertNotNull(s3Client);

		// Setting up
		
		createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		// strictly speaking it's nonsensical for a group to be a 'modifier'.  we're just using it for testing purposes
		modifiedById = BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();
		
		// create a resource on which to apply permissions
		node = new Node();
		node.setName("foo");
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(createdById);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(modifiedById);
		node.setNodeType(EntityType.project.name());
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

		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(node.getId());
		acl.setCreationDate(new Date(System.currentTimeMillis()));

		Set<ResourceAccess> ras =
				AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(
						Long.parseLong(group.getId()), Long.parseLong(group2.getId())), 2, false);
		acl.setResourceAccess(ras);

		String aclId = aclDao.create(acl, ObjectType.ENTITY);
		assertEquals(node.getId(), aclId);
		aclList.add(acl);

		assertTrue(waitForObject(aclKeys, resourceAccessKeys, 1, 1));
		String key = getNewAclKey(aclKeys);
		assertNotNull(key);
		List<AclRecord> aclRecords = aclRecordDao.getBatch(key);
		assertEquals(1, aclRecords.size());
		AclRecord aclRecord = aclRecords.get(0);
		assertNotNull(aclRecord);
		assertEquals(ObjectType.ENTITY, aclRecord.getOwnerType());
		assertEquals(node.getId(), aclRecord.getOwnerId());
		assertEquals(ChangeType.CREATE, aclRecord.getChangeType());

		key = getNewResourceAccessRecordKey(resourceAccessKeys);
		assertNotNull(key);
		List<ResourceAccessRecord> raRecords = resourceAccessRecordDao.getBatch(key);
		assertEquals(4, raRecords.size());
		assertEquals(ras, AclSnapshotWorkerTestUtils.getSetOfResourceAccess(raRecords));
		assertEquals(aclRecord.getChangeNumber(), raRecords.get(0).getChangeNumber());
	}

	@Test
	public void testUpdate() throws Exception {
		Set<String> aclKeys = aclRecordDao.listAllKeys();
		Set<String> resourceAccessKeys = resourceAccessRecordDao.listAllKeys();
		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(node.getId());
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		Set<ResourceAccess> ras =
				AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(
						Long.parseLong(group.getId()), Long.parseLong(group2.getId())), 2, false);
		acl.setResourceAccess(ras);
		String aclId = aclDao.create(acl, ObjectType.ENTITY);
		assertEquals(node.getId(), aclId);
		aclList.add(acl);
		assertTrue(waitForObject(aclKeys, resourceAccessKeys, 1, 1));

		aclKeys = aclRecordDao.listAllKeys();
		resourceAccessKeys = resourceAccessRecordDao.listAllKeys();
		// Update ACL for this node
		acl = aclDao.get(node.getId(), ObjectType.ENTITY);
		assertNotNull(acl);
		assertNotNull(acl.getEtag());
		assertEquals(node.getId(), acl.getId());
		ras = AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(
				Long.parseLong(group.getId()), Long.parseLong(group2.getId())), 2, true);
		acl.setResourceAccess(ras);

		aclDao.update(acl, ObjectType.ENTITY);
		aclList.add(acl);

		assertTrue(waitForObject(aclKeys, resourceAccessKeys, 1, 1));

		String key = getNewAclKey(aclKeys);
		assertNotNull(key);
		List<AclRecord> aclRecords = aclRecordDao.getBatch(key);
		assertEquals(1, aclRecords.size());
		AclRecord aclRecord = aclRecords.get(0);
		assertNotNull(aclRecord);
		assertEquals(ObjectType.ENTITY, aclRecord.getOwnerType());
		assertEquals(node.getId(), aclRecord.getOwnerId());
		assertEquals(ChangeType.UPDATE, aclRecord.getChangeType());
		
		key = getNewResourceAccessRecordKey(resourceAccessKeys);
		assertNotNull(key);
		List<ResourceAccessRecord> raRecords = resourceAccessRecordDao.getBatch(key);
		assertEquals(4, raRecords.size());

		assertEquals(ras, AclSnapshotWorkerTestUtils.getSetOfResourceAccess(raRecords));
		assertEquals(aclRecord.getChangeNumber(), raRecords.get(0).getChangeNumber());
	}

	@Test
	public void testDelete() throws Exception {
		Set<String> aclKeys = aclRecordDao.listAllKeys();
		Set<String> resourceAccessKeys = resourceAccessRecordDao.listAllKeys();
		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(node.getId());
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		Set<ResourceAccess> ras =
				AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(
						Long.parseLong(group.getId()), Long.parseLong(group2.getId())), 2, false);
		acl.setResourceAccess(ras);
		String aclId = aclDao.create(acl, ObjectType.ENTITY);
		assertEquals(node.getId(), aclId);
		aclList.add(acl);
		assertTrue(waitForObject(aclKeys, resourceAccessKeys, 1, 1));

		aclKeys = aclRecordDao.listAllKeys();
		resourceAccessKeys = resourceAccessRecordDao.listAllKeys();

		// Delete the acl
		aclDao.delete(node.getId(), ObjectType.ENTITY);

		assertTrue(waitForObject(aclKeys, resourceAccessKeys, 1, 0));
		String key = getNewAclKey(aclKeys);
		assertNotNull(key);
		List<AclRecord> aclRecords = aclRecordDao.getBatch(key);
		assertEquals(1, aclRecords.size());
		AclRecord aclRecord = aclRecords.get(0);
		assertNotNull(aclRecord);
		assertNull(aclRecord.getOwnerId());
		assertNull(aclRecord.getOwnerType());
		assertEquals(ChangeType.DELETE, aclRecord.getChangeType());
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
	 * Helper method that continue looking into s3 bucket and find noAcl number
	 * of new AclRecord log files compare to aclKeys - the list of old AclRecord
	 * log files, and noRA number of new ResourceAccessRecord log files compare
	 * to resourceAccessKeys - the list of old ResourceAccessRecord log files.
	 * 
	 * @return true if found what was looking for in TIME_OUT milliseconds,
	 *         false otherwise.
	 */
	private boolean waitForObject(Set<String> aclKeys, Set<String> resourceAccessKeys, int noAcl, int noRA) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start + TIME_OUT) {
			Set<String> newAclKeys = aclRecordDao.listAllKeys();
			Set<String> newResourceKeys = resourceAccessRecordDao.listAllKeys();

			newAclKeys.removeAll(aclKeys);
			newResourceKeys.removeAll(resourceAccessKeys);

			if (noAcl == newAclKeys.size() && noRA == newResourceKeys.size()){
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the first new Acl key in S3 compare to old aclKeys
	 */
	private String getNewAclKey(Set<String> aclKeys) {
		Set<String> newAclKeys = aclRecordDao.listAllKeys();
		newAclKeys.removeAll(aclKeys);
		return new ArrayList<String>(newAclKeys).get(0);
	}

	/**
	 * @return the first new ResourceAccess key in S3 compare to old resourceAccessKeys
	 */
	private String getNewResourceAccessRecordKey(Set<String> resourceAccessKeys) {
		Set<String> newResourceKeys = resourceAccessRecordDao.listAllKeys();
		newResourceKeys.removeAll(resourceAccessKeys);
		return new ArrayList<String>(newResourceKeys).get(0);
	}
}
