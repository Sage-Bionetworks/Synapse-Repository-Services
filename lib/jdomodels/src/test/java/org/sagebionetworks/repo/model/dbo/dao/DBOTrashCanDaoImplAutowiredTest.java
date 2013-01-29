package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOTrashCanDaoImplAutowiredTest {

	@Autowired
	private DBOTrashCanDao trashCanDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	private long userId;

	@Before
	public void before() throws Exception {

		userId = Long.parseLong(userGroupDAO.findGroup(
				AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		assertNotNull(userId);

		clear();

		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userId, 0L, Long.MAX_VALUE);
		assertTrue(trashList.size() == 0);
	}

	@After
	public void after() throws Exception {
		clear();
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userId, 0L, Long.MAX_VALUE);
		assertTrue(trashList.size() == 0);
	}

	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException {

		long nodeId1 = 555L;
		long parentId1 = 5L;
		trashCanDao.create(userId, nodeId1, parentId1);
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userId, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(1, trashList.size());
		TrashedEntity trash = trashList.get(0);
		assertEquals(KeyFactory.keyToString(nodeId1), trash.getEntityId());
		assertEquals(Long.toString(userId), trash.getDeletedByPrincipalId());
		assertEquals(KeyFactory.keyToString(parentId1), trash.getOriginalParentId());
		assertNotNull(trash.getDeletedOn());

		long nodeId2 = 666L;
		long parentId2 = 6L;
		trashCanDao.create(userId, nodeId2, parentId2);
		trashList = trashCanDao.getInRangeForUser(userId, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(2, trashList.size());

		trashCanDao.delete(userId, nodeId1);
		trashList = trashCanDao.getInRangeForUser(userId, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(1, trashList.size());
		trash = trashList.get(0);
		assertEquals(KeyFactory.keyToString(nodeId2), trash.getEntityId());
		assertEquals(Long.toString(userId), trash.getDeletedByPrincipalId());
		assertEquals(KeyFactory.keyToString(parentId2), trash.getOriginalParentId());
		assertNotNull(trash.getDeletedOn());

		trashCanDao.delete(userId, nodeId2);
		trashList = trashCanDao.getInRangeForUser(userId, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(0, trashList.size());
	}

	private void clear() throws Exception {
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userId, 0L, Long.MAX_VALUE);
		for (TrashedEntity trash : trashList) {
			trashCanDao.delete(userId, KeyFactory.stringToKey(trash.getEntityId()));
		}
	}
}
