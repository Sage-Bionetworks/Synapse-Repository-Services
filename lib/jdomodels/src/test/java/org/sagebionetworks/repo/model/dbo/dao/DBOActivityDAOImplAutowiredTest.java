package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.Used;
import org.sagebionetworks.repo.model.provenance.UsedEntity;
import org.sagebionetworks.repo.model.provenance.UsedURL;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;

/**
 * This test of the DBOActivityDAOImpl is only for DB function and DB enforced 
 * business logic. Put tests for DAO business logic in DBOActivityDAOImplTest
 * @author dburdick
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOActivityDAOImplAutowiredTest {

	@Autowired
	ActivityDAO activityDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private NodeDAO nodeDao;
	
	// the activity ids that must be deleted at the end of each test.
	List<String> toDelete;
	List<String> nodesToDelete;
	
	private UserInfo userInfo = null;
	private Long creatorUserGroupId = null;	
	private Long altUserGroupId = null;
	@Before
	public void before() throws Exception {
		toDelete = new ArrayList<String>();
		nodesToDelete = new ArrayList<String>();
		
		creatorUserGroupId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		assertNotNull(creatorUserGroupId);
		
		altUserGroupId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name(), false).getId());
		assertNotNull(altUserGroupId);
		
		assertNotNull(activityDao);
		toDelete = new ArrayList<String>();
	}
	
	@After
	public void after() throws DatastoreException{
		// delete activites
		if(toDelete != null && activityDao != null){
			for(String id : toDelete){
				// Delete each				
				activityDao.delete(id);				
			}
		}
		
		// delete nodes
		if(nodesToDelete != null && nodeDao != null){
			for(String id:  nodesToDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (NotFoundException e) {
					// happens if the object no longer exists.
				}
			}
		}

	}
		
	@Test 
	public void testCreate() throws Exception{
		long initialCount = activityDao.getCount();
		Activity toCreate = newTestActivity(idGenerator.generateNewId().toString());
		String id = activityDao.create(toCreate);				
		assertEquals(1+initialCount, activityDao.getCount()); 
		toDelete.add(id);
		assertNotNull(id);
		
		// This activity should exist & make sure we can fetch it		
		Activity loaded = activityDao.get(id);
		assertEquals(id, loaded.getId().toString());
		assertNotNull(loaded.getEtag());			
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithExistingId() throws Exception{
		String sameId = idGenerator.generateNewId().toString();
		Activity toCreate = newTestActivity(sameId);
		String id = activityDao.create(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Activity duplicate = newTestActivity(sameId);
		// This should throw an exception.
		String duplicateId = activityDao.create(duplicate);

		// shouldn't get here, but clean up and fail just in case
		toDelete.add(duplicateId);
		fail();
	}

	@Test
	public void testUpdate() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());		
		activityDao.create(act);
		toDelete.add(act.getId().toString());
		String firstEtag = act.getEtag();
		// update activity
		String desc = "some desc";
		assertTrue(!desc.equals(act.getDescription()));
		act.setDescription(desc);
		Activity updatedAct = activityDao.update(act);
		assertEquals(desc, updatedAct.getDescription());
		assertEquals(firstEtag, updatedAct.getEtag());
	}

	@Test
	public void testGet() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());		
		activityDao.create(act);
		toDelete.add(act.getId().toString());
		Activity getAct = activityDao.get(act.getId().toString());		
		assertEquals(act.getId(), getAct.getId());
		assertEquals(act.getDescription(), getAct.getDescription());
		assertEquals(act.getUsed(), getAct.getUsed());
		assertEquals(act.getCreatedBy(), getAct.getCreatedBy());
		assertEquals(act.getCreatedOn(), getAct.getCreatedOn());
		assertEquals(act.getModifiedBy(), getAct.getModifiedBy());
		assertEquals(act.getModifiedOn(), getAct.getModifiedOn());
		// transient things that should be replaced by dao
		assertFalse(act.getEtag().equals(getAct.getEtag()));
	}
	
	@Test(expected=NotFoundException.class)
	public void testDelete() throws Exception {
		String id = idGenerator.generateNewId().toString();
		Activity act = newTestActivity(id);		
		activityDao.create(act);
		toDelete.add(id.toString()); // just in case
		activityDao.delete(id);
		
		// should throw notfoundexception
		activityDao.get(id);
		fail();
	}
	
 	// Calling lockActivityAndIncrementEtag() outside of a transaction in not allowed, and will throw an exception.
	@Test(expected=IllegalTransactionStateException.class)
	public void testLockActivityAndIncrementEtagNoTransaction() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());
		String id = activityDao.create(act);
		toDelete.add(id);
		assertNotNull(id);
		String eTag = act.getEtag();
		activityDao.lockActivityAndGenerateEtag(id, eTag, ChangeType.UPDATE);
		fail("Should have thrown an IllegalTransactionStateException");
	}

	@Test
	public void testDoesActivityExist() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());		
		activityDao.create(act);
		toDelete.add(act.getId().toString());
		assertTrue(activityDao.doesActivityExist(act.getId().toString()));				
	}

	@Test
	public void testDoesActivityExistNotFound() throws Exception {
		assertFalse(activityDao.doesActivityExist("unknown id"));				
	}

	@Test
	public void testGetEntitiesGeneratedBy() throws Exception {
		// create two activites
		Activity act1 = newTestActivity(idGenerator.generateNewId().toString());		
		activityDao.create(act1);
		toDelete.add(act1.getId().toString());
		
		creatorUserGroupId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		assertNotNull(creatorUserGroupId);

		// make two version of node1, generated by act1  
		Node node1 = NodeTestUtils.createNew("generated1", creatorUserGroupId, creatorUserGroupId);
		node1.setActivityId(act1.getId());
		String node1Id = nodeDao.createNew(node1);		
		nodesToDelete.add(node1Id);
		node1 = nodeDao.getNode(node1Id);
		node1.setVersionLabel("v2");
		node1.setVersionNumber(2L);
		node1.setActivityId(act1.getId());
		nodeDao.createNewVersion(node1);

		// make a second node generated by act1 
		Node node2 = NodeTestUtils.createNew("generated2", creatorUserGroupId, creatorUserGroupId);
		node2.setActivityId(act1.getId());
		String node2Id = nodeDao.createNew(node2);
		nodesToDelete.add(node2Id);

		// create verify References
		Reference refNode1v1 = new Reference();
		refNode1v1.setTargetId(node1Id);
		refNode1v1.setTargetVersionNumber(1L);		
		Reference refNode1v2 = new Reference();
		refNode1v2.setTargetId(node1Id);
		refNode1v2.setTargetVersionNumber(2L);		
		Reference refNode2v1 = new Reference();
		refNode2v1.setTargetId(node2Id);
		refNode2v1.setTargetVersionNumber(1L);		
		
		// get all at once		
		int limit = 10;
		int offset = 0;
		PaginatedResults<Reference> results = activityDao.getEntitiesGeneratedBy(act1.getId().toString(), limit, offset);
		assertEquals(3, results.getResults().size());		
		assertEquals(3, results.getTotalNumberOfResults());
		assertTrue(results.getResults().contains(refNode1v1));
		assertTrue(results.getResults().contains(refNode1v2));
		assertTrue(results.getResults().contains(refNode2v1));
		
		// test two pages
		limit = 1;
		offset = 0;
		results = activityDao.getEntitiesGeneratedBy(act1.getId().toString(), limit, offset);
		assertEquals(1, results.getResults().size());		
		assertEquals(3, results.getTotalNumberOfResults());
		limit = 1;
		offset = 1;
		results = activityDao.getEntitiesGeneratedBy(act1.getId().toString(), limit, offset);
		assertEquals(1, results.getResults().size());		
		assertEquals(3, results.getTotalNumberOfResults());
		
		// empty result
		limit = 0;
		offset = 0;
		results = activityDao.getEntitiesGeneratedBy(act1.getId().toString(), limit, offset);
		assertEquals(0, results.getResults().size());		
		assertEquals(3, results.getTotalNumberOfResults());				
	}
		
	@Test
	public void testDeleteActivityRevisionForeignKeyConstraint() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());		
		activityDao.create(act);
		toDelete.add(act.getId().toString());
		creatorUserGroupId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		assertNotNull(creatorUserGroupId);

		Node toCreate = NodeTestUtils.createNew("soonToNotHaveActivity", creatorUserGroupId, creatorUserGroupId);
		toCreate.setActivityId(act.getId());
		String nodeId = nodeDao.createNew(toCreate);
		nodesToDelete.add(nodeId);
		Node createdNode = nodeDao.getNode(nodeId);
		// assure that activity is set as a baseline for the rest of this test
		assertNotNull(createdNode.getActivityId());		
		
		// delete activity
		activityDao.delete(act.getId());
		
		// assure that activity id was set to null with deletion of activity
		Node alteredNode = nodeDao.getNode(nodeId);
		assertNull(alteredNode.getActivityId());
	}
	
	@Test
	public void testGetMigrationObjectData() throws Exception {
		long offset;
		long limit;
		int expectedSize;
		int numActs = 11;
		generateActivities(numActs);
		
		// try simple paging
		offset = 0;
		limit = 10;
		expectedSize = 10;
		verifyResultSize(offset, limit, expectedSize, numActs);
		offset += limit;
		expectedSize = 1;
		verifyResultSize(offset, limit, expectedSize, numActs);

		// get none
		offset = 0;
		limit = 0;
		expectedSize = 0;
		verifyResultSize(offset, limit, expectedSize, numActs);

		// get all in one
		offset = 0;
		limit = numActs;
		expectedSize = numActs;
		verifyResultSize(offset, limit, expectedSize, numActs);
	}

	
	
	@Test
	public void testCreateFromBackup() throws Exception {
		long initialCount = activityDao.getCount();
		Activity toCreate = newTestActivity(idGenerator.generateNewId().toString());
		String startEtag = "real";
		toCreate.setEtag(startEtag);
		String id = activityDao.createFromBackup(toCreate);				
		assertEquals(1+initialCount, activityDao.getCount()); 
		toDelete.add(id);
		assertNotNull(id);
		
		// This activity should exist & make sure we can fetch it		
		Activity loaded = activityDao.get(id);
		assertEquals(id, loaded.getId().toString());
		assertEquals(startEtag, loaded.getEtag());				
	}

	
	/*
	 * Private Methods
	 */
	private Activity newTestActivity(String id) {		
		Activity act = new Activity();
		act.setId(id);
		act.setEtag("0");	
		act.setDescription("description");
		act.setCreatedBy(altUserGroupId.toString());
		act.setCreatedOn(new Date());
		act.setModifiedBy(altUserGroupId.toString());
		act.setModifiedOn(new Date());
		UsedEntity usedEnt = new UsedEntity();
		Reference ref = new Reference();
		ref.setTargetId("syn123");
		ref.setTargetVersionNumber((long)1);
		usedEnt.setReference(ref);
		usedEnt.setWasExecuted(true);
		Set<Used> used = new HashSet<Used>();
		used.add(usedEnt);
		UsedURL ux = new UsedURL();
		ux.setUrl("http://url.com");
		used.add(ux);
		act.setUsed(used);
		return act;
	}

	private void generateActivities(int numActs) {
		// add 3 entities
		for(int i=0; i<numActs; i++) {
			Activity act = newTestActivity(idGenerator.generateNewId().toString());
			activityDao.create(act);
			toDelete.add(act.getId());			
		}
	}

	private void verifyResultSize(long offset, long limit, int expectedSize, int totalResults) {
		QueryResults<MigratableObjectData> results = activityDao.getMigrationObjectData(offset, limit, false);
		List<MigratableObjectData> ods = results.getResults();
		assertEquals(expectedSize, ods.size());
		assertEquals(totalResults, results.getTotalNumberOfResults());
	}	

}
