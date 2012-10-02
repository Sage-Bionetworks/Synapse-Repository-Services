package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.ActivityType;
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
	private ETagGenerator eTagGenerator;
		
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
				try{
					activityDao.delete(id);
				}catch (NotFoundException e) {
					// happens if the object no longer exists.
				}
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
		Activity actCreated = activityDao.create(toCreate);		
		String id = actCreated.getId().toString();
		assertEquals(1+initialCount, activityDao.getCount()); 
		toDelete.add(id);
		assertNotNull(id);
		
		// This activity should exist & make sure we can fetch it		
		Activity loaded = activityDao.get(id);
		assertEquals(id, loaded.getId().toString());
		assertNotNull(loaded.getEtag());
		
		checkMigrationDependenciesNoParentDistinctModifier(id);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithExistingId() throws Exception{
		String sameId = idGenerator.generateNewId().toString();
		Activity toCreate = newTestActivity(sameId);
		Activity created = activityDao.create(toCreate);
		String id = created.getId().toString();
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Activity duplicate = newTestActivity(sameId);
		// This should throw an exception.
		Activity duplicateCreated = activityDao.create(duplicate);

		// shouldn't get here, but clean up and fail just in case
		toDelete.add(duplicateCreated.getId().toString());
		fail();
	}

	@Test
	public void testUpdate() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());		
		act = activityDao.create(act);
		toDelete.add(act.getId().toString());
		// update activity
		assertTrue(!act.getActivityType().equals(ActivityType.CODE_EXECUTION));
		act.setActivityType(ActivityType.CODE_EXECUTION);
		String desc = "some desc";
		assertTrue(!desc.equals(act.getDescription()));
		act.setDescription(desc);
		Activity updatedAct = activityDao.update(act);
		assertEquals(ActivityType.CODE_EXECUTION, updatedAct.getActivityType());
		assertEquals(desc, updatedAct.getDescription());		
	}

	@Test
	public void testGet() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());		
		act = activityDao.create(act);
		toDelete.add(act.getId().toString());
		Activity getAct = activityDao.get(act.getId().toString());		
		assertEquals(act, getAct);
	}
	
	@Test(expected=NotFoundException.class)
	public void testDelete() throws Exception {
		String id = idGenerator.generateNewId().toString();
		Activity act = newTestActivity(id);		
		act = activityDao.create(act);
		toDelete.add(id.toString()); // just in case
		activityDao.delete(act.getId().toString());
		
		// should throw notfoundexception
		activityDao.get(id.toString());
		fail();
	}
	
	@Test
	public void testGetIds() throws Exception {
		Activity act1 = newTestActivity(idGenerator.generateNewId().toString());
		act1 = activityDao.create(act1);
		toDelete.add(act1.getId().toString());
		
		Activity act2 = newTestActivity(idGenerator.generateNewId().toString());
		act2 = activityDao.create(act2);
		toDelete.add(act2.getId().toString());
		
		List<String> actualIds = activityDao.getIds();
		assertTrue(actualIds.contains(act1.getId().toString()));
		assertTrue(actualIds.contains(act2.getId().toString()));		
	}

//	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
//	@Test
//	public void testLockActivityAndIncrementEtag() throws Exception {
//		Activity act = newTestActivity(idGenerator.generateNewId().toString());
//		act = activityDao.create(act);
//		String id = act.getId().toString();		
//		toDelete.add(id);
//		assertNotNull(id);
//		String eTag1 = act.getEtag();
//		String eTag2returned = activityDao.lockActivityAndIncrementEtag(id, eTag1, ChangeType.UPDATE);
//				
//		Activity actUpdated = activityDao.get(id);
//		String eTag2 = actUpdated.getEtag();
//		assertTrue(eTag1.equals(eTag2returned));
//		assertTrue(eTag1.equals(eTag2));
//	}
	
 	// Calling lockActivityAndIncrementEtag() outside of a transaction in not allowed, and will throw an exception.
	@Test(expected=IllegalTransactionStateException.class)
	public void testLockActivityAndIncrementEtagNoTransaction() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());
		act = activityDao.create(act);
		String id = act.getId().toString();
		toDelete.add(id);
		assertNotNull(id);
		String eTag = act.getEtag();
		eTag = activityDao.lockActivityAndIncrementEtag(id, eTag, ChangeType.UPDATE);
		fail("Should have thrown an IllegalTransactionStateException");
	}

	@Test
	public void testDoesActivityExist() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());		
		act = activityDao.create(act);
		toDelete.add(act.getId().toString());
		assertTrue(activityDao.doesActivityExist(act.getId().toString()));				
	}

	@Test
	public void testDoesActivityExistNotFound() throws Exception {
		assertTrue(!activityDao.doesActivityExist("unknown id"));				
	}

	@Test
	public void testGetEntitiesGeneratedBy() throws Exception {
		Activity act = newTestActivity(idGenerator.generateNewId().toString());		
		act = activityDao.create(act);
		toDelete.add(act.getId().toString());
		creatorUserGroupId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		assertNotNull(creatorUserGroupId);

		Node toCreate = NodeTestUtils.createNew("generated1", creatorUserGroupId, creatorUserGroupId);
		toCreate.setActivityId(act.getId());
		String nodeId = nodeDao.createNew(toCreate);
		nodesToDelete.add(nodeId);
	
		List<Reference> generatedBy = activityDao.getEntitiesGeneratedBy(act.getId().toString());
		//TODO: finish
	}
		
	
	/*
	 * Private Methods
	 */
	private Activity newTestActivity(String id) {		
		Activity act = new Activity();
		act.setId(id);
		act.setEtag("0");	
		act.setDescription("description");
		act.setCreatedBy("555");
		act.setCreatedOn(new Date());
		act.setModifiedBy("666");
		act.setModifiedOn(new Date());
		act.setActivityType(ActivityType.UNDEFINED);
		Reference ref = new Reference();
		ref.setTargetId("syn123");
		ref.setTargetVersionNumber((long)1);
		Set<Reference> used = new HashSet<Reference>();
		used.add(ref);
		act.setUsed(used);
		Reference executedEntity = new Reference();
		executedEntity.setTargetId("syn456");
		executedEntity.setTargetVersionNumber((long)1);
		act.setExecutedEntity(executedEntity);
		return act;
	}

	private void checkMigrationDependenciesNoParentDistinctModifier(String id) throws Exception {
		// first check what happens if dependencies are NOT requested
		QueryResults<MigratableObjectData> results = activityDao.getMigrationObjectData(0, 10000, false);
		List<MigratableObjectData> ods = results.getResults();
		assertEquals(ods.size(), results.getTotalNumberOfResults());
		assertTrue(ods.size()>0);
		boolean foundId = false;
		for (MigratableObjectData od : ods) {
			if (od.getId().getId().equals(id)) {
				foundId=true;
			}
			assertEquals(MigratableObjectType.ACTIVITY, od.getId().getType());
			
		}
		assertTrue(foundId);
		
		// Activities have no dependencies, so same test
		results = activityDao.getMigrationObjectData(0, 10000, true);
		ods = results.getResults();
		assertEquals(ods.size(), results.getTotalNumberOfResults());
		assertTrue(ods.size()>0);
		foundId = false;
		for (MigratableObjectData od : ods) {
			if (od.getId().getId().equals(id)) {
				foundId=true;
			}
			assertEquals(MigratableObjectType.ACTIVITY, od.getId().getType());
			
		}
		assertTrue(foundId);
	
	}
	

}
