package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.UuidETagGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOTrashCanBackupDaoImplAutowiredTest {

	@Autowired
	private DBOTrashCanDao trashCanDao;

	@Autowired
	private DBOTrashCanBackupDao trashCanBackupDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	private String userId;

	@Before
	public void before() throws Exception {

		userId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(userId);

		clear();
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userId, 0L, Long.MAX_VALUE);
		assertTrue(trashList.size() == 0);
		assertEquals(0L, trashCanBackupDao.getCount());
	}

	@After
	public void after() throws Exception {
		clear();
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userId, 0L, Long.MAX_VALUE);
		assertTrue(trashList.size() == 0);
		assertEquals(0L, trashCanBackupDao.getCount());
	}

	@Test
	public void testGetAndDelete() throws DatastoreException {

		final String entityId = KeyFactory.keyToString(555L);
		assertEquals(0L, trashCanBackupDao.getCount());
		TrashedEntity entity = trashCanBackupDao.get(entityId);
		assertNull(entity);

		final String parentId = KeyFactory.keyToString(333L);
		trashCanDao.create(userId, entityId, parentId);
		assertEquals(1L, trashCanBackupDao.getCount());
		entity = trashCanBackupDao.get(entityId);
		assertNotNull(entity);
		assertEquals(entityId, entity.getEntityId());
		assertEquals(userId, entity.getDeletedByPrincipalId());
		assertEquals(parentId, entity.getOriginalParentId());
		assertNotNull(entity.getDeletedOn());

		trashCanBackupDao.delete(KeyFactory.keyToString(37283747820L));
		assertEquals(1L, trashCanBackupDao.getCount());
		trashCanBackupDao.delete(entityId);
		assertEquals(0L, trashCanBackupDao.getCount());
		entity = trashCanBackupDao.get(entityId);
		assertNull(entity);
	}

	@Test
	public void testUpdate() throws DatastoreException {

		// Before update(), there should be nothing there
		final String entityId = KeyFactory.keyToString(555L);
		final String parentId = KeyFactory.keyToString(333L);
		assertEquals(0L, trashCanBackupDao.getCount());
		TrashedEntity trashBack = trashCanBackupDao.get(entityId);
		assertNull(trashBack);

		// Since nothing is there, update() should create
		TrashedEntity trash = new TrashedEntity();
		trash.setEntityId(entityId);
		trash.setDeletedByPrincipalId(userId);
		trash.setOriginalParentId(parentId);
		trash.setDeletedOn(new Date());
		trashCanBackupDao.update(trash);
		assertEquals(1L, trashCanBackupDao.getCount());
		trashBack = trashCanBackupDao.get(entityId);
		assertNotNull(trashBack);
		assertEquals(entityId, trashBack.getEntityId());
		assertEquals(userId, trashBack.getDeletedByPrincipalId());
		assertEquals(parentId, trashBack.getOriginalParentId());
		assertNotNull(trashBack.getDeletedOn());

		// Now update() by changing it
		final String newParentId = KeyFactory.keyToString(111L);
		trash.setOriginalParentId(newParentId);
		trashCanBackupDao.update(trash);
		assertEquals(1L, trashCanBackupDao.getCount());
		trashBack = trashCanBackupDao.get(entityId);
		assertNotNull(trashBack);
		assertEquals(entityId, trashBack.getEntityId());
		assertEquals(userId, trashBack.getDeletedByPrincipalId());
		assertEquals(newParentId, trashBack.getOriginalParentId());
		assertNotNull(trashBack.getDeletedOn());

		// Changing the entity ID is changing the primary key. It should create.
		final String newEntityId = KeyFactory.keyToString(111L);
		trash.setEntityId(newEntityId);
		trashCanBackupDao.update(trash);
		assertEquals(2L, trashCanBackupDao.getCount());
		trashBack = trashCanBackupDao.get(entityId);
		assertNotNull(trashBack);
		assertEquals(entityId, trashBack.getEntityId());
		assertEquals(userId, trashBack.getDeletedByPrincipalId());
		assertEquals(newParentId, trashBack.getOriginalParentId());
		assertNotNull(trashBack.getDeletedOn());
		trashBack = trashCanBackupDao.get(newEntityId);
		assertNotNull(trashBack);
		assertEquals(newEntityId, trashBack.getEntityId());
		assertEquals(userId, trashBack.getDeletedByPrincipalId());
		assertEquals(newParentId, trashBack.getOriginalParentId());
		assertNotNull(trashBack.getDeletedOn());
	}

	@Test
	public void testGetMigrationObjectData() {

		assertEquals(MigratableObjectType.TRASHED_ENTITY, trashCanBackupDao.getMigratableObjectType());

		QueryResults<MigratableObjectData> results =  trashCanBackupDao.getMigrationObjectData(0, Long.MAX_VALUE, true);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		final String entityId = KeyFactory.keyToString(555L);
		final String parentId = KeyFactory.keyToString(333L);
		trashCanDao.create(userId, entityId, parentId);

		results =  trashCanBackupDao.getMigrationObjectData(0, Long.MAX_VALUE, true);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults());
		assertEquals(1, results.getResults().size());
		MigratableObjectData obj = results.getResults().get(0);
		assertEquals(KeyFactory.stringToKey(entityId).toString(), obj.getId().getId());
		assertEquals(MigratableObjectType.TRASHED_ENTITY, obj.getId().getType());
		assertEquals(UuidETagGenerator.ZERO_E_TAG, obj.getEtag());

		trashCanDao.create(userId, KeyFactory.keyToString(777L), KeyFactory.keyToString(333L));
		results =  trashCanBackupDao.getMigrationObjectData(0, Long.MAX_VALUE, true);
		assertNotNull(results);
		assertEquals(2, results.getTotalNumberOfResults());
		assertEquals(2, results.getResults().size());
	}

	private void clear() throws Exception {
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userId, 0L, Long.MAX_VALUE);
		for (TrashedEntity trash : trashList) {
			trashCanDao.delete(userId, trash.getEntityId());
		}
	}
}
