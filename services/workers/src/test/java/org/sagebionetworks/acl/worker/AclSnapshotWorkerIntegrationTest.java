package org.sagebionetworks.acl.worker;

import static org.junit.Assert.*;

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
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
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
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

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
	
	private Long createdById;
	private Long modifiedById;
	
	private String aclRecordBucket;
	private String resourceAccessRecordBucket;
	
	private int TIME_OUT = 30 * 1000;
	
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

		aclRecordBucket = config.getAclRecordBucketName();
		resourceAccessRecordBucket = config.getResourceAccessRecordBucketName();
		
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
	public void test() throws Exception {
		
		// Test CREATE
		
		List<String> aclKeys = getKeys(aclRecordBucket);
		List<String> resourceAccessKeys = getKeys(resourceAccessRecordBucket);
		
		assertNotNull(aclKeys);
		assertNotNull(resourceAccessKeys);
		
		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(node.getId());
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		String aclId = aclDao.create(acl, ObjectType.ENTITY);
		assertEquals(node.getId(), aclId);

		assertTrue(waitForObject(aclKeys, resourceAccessKeys, 1, 0));
		String key = getNewAclKey(aclKeys);
		assertNotNull(key);
		List<AclRecord> aclRecords = aclRecordDao.getBatch(key);
		assertEquals(1, aclRecords.size());
		AclRecord aclRecord = aclRecords.get(0);
		assertNotNull(aclRecord);
		assertEquals(ObjectType.ENTITY, aclRecord.getOwnerType());
		assertEquals(node.getId(), aclRecord.getOwnerId());
		assertEquals(ChangeType.CREATE, aclRecord.getChangeType());
	
		// Test UPDATE
		
		aclKeys = getKeys(aclRecordBucket);
		resourceAccessKeys = getKeys(resourceAccessRecordBucket);
		
		// Update ACL for this node
		acl = aclDao.get(node.getId(), ObjectType.ENTITY);
		assertNotNull(acl);
		assertNotNull(acl.getEtag());
		assertEquals(node.getId(), acl.getId());
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(group.getId()));
		ra.setAccessType(new HashSet<ACCESS_TYPE>(
				Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD
				})));
		ras.add(ra);
		acl.setResourceAccess(ras);

		aclDao.update(acl, ObjectType.ENTITY);

		assertTrue(waitForObject(aclKeys, resourceAccessKeys, 1, 1));
		
		key = getNewAclKey(aclKeys);
		assertNotNull(key);
		aclRecords = aclRecordDao.getBatch(key);
		assertEquals(1, aclRecords.size());
		aclRecord = aclRecords.get(0);
		assertNotNull(aclRecord);
		assertEquals(ObjectType.ENTITY, aclRecord.getOwnerType());
		assertEquals(node.getId(), aclRecord.getOwnerId());
		assertEquals(ChangeType.UPDATE, aclRecord.getChangeType());
		
		key = getNewResourceAccessRecordKey(resourceAccessKeys);
		assertNotNull(key);
		List<ResourceAccessRecord> raRecords = resourceAccessRecordDao.getBatch(key);
		assertEquals(1, raRecords.size());
		ResourceAccessRecord raRecord = raRecords.get(0);
		assertNotNull(raRecord);
		assertEquals(group.getId(), raRecord.getPrincipalId().toString());
		assertEquals(ra.getAccessType(), raRecord.getAccessType());
		assertEquals(aclRecord.getChangeNumber(), raRecord.getChangeNumber());

		// Test DELETE
	
		aclKeys = getKeys(aclRecordBucket);
		resourceAccessKeys = getKeys(resourceAccessRecordBucket);
			
		// Delete the acl
		aclDao.delete(node.getId(), ObjectType.ENTITY);

		assertTrue(waitForObject(aclKeys, resourceAccessKeys, 1, 0));
		key = getNewAclKey(aclKeys);
		assertNotNull(key);
		aclRecords = aclRecordDao.getBatch(key);
		assertEquals(1, aclRecords.size());
		aclRecord = aclRecords.get(0);
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
		groupList.clear();

		aclRecordDao.deleteAllStackInstanceBatches();
		resourceAccessRecordDao.deleteAllStackInstanceBatches();
	}
	
	private List<String> getKeys(String bucketName) {
		ObjectListing listing = s3Client.listObjects(bucketName);
		List<S3ObjectSummary> summaries = listing.getObjectSummaries();
		List<String> keys = new ArrayList<String>();
 
		while (listing.isTruncated()) {
		   listing = s3Client.listNextBatchOfObjects (listing);
		   summaries.addAll (listing.getObjectSummaries());
		}

		for (S3ObjectSummary summary : summaries) {
			keys.add(summary.getKey());
		}
		return keys;
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
	private boolean waitForObject(List<String> aclKeys, List<String> resourceAccessKeys, int noAcl, int noRA) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start + TIME_OUT) {
			List<String> newAclKeys = getKeys(aclRecordBucket);
			List<String> newResourceKeys = getKeys(resourceAccessRecordBucket);

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
	private String getNewAclKey(List<String> aclKeys) {
		List<String> newAclKeys = getKeys(aclRecordBucket);
		newAclKeys.removeAll(aclKeys);
		return newAclKeys.get(0);
	}

	/**
	 * @return the first new ResourceAccess key in S3 compare to old resourceAccessKeys
	 */
	private String getNewResourceAccessRecordKey(List<String> resourceAccessKeys) {
		List<String> newResourceKeys = getKeys(resourceAccessRecordBucket);
		newResourceKeys.removeAll(resourceAccessKeys);
		return newResourceKeys.get(0);
	}
}
