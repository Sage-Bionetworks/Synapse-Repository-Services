package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOTrashCanDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TrashCanBackupDriverTest {

	@Autowired
	private DBOTrashCanDao trashCanDao;

	@Autowired
	private TrashCanBackupDriver trashCanBackupDriver;

	@Autowired
	private UserGroupDAO userGroupDAO;

	private String userId;

	@Before
	public void before() throws Exception {
		assertNotNull(trashCanDao);
		assertNotNull(trashCanBackupDriver);
		assertNotNull(userGroupDAO);
		userId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(userId);
		cleanUp(); // For leftovers from other test cases
	}

	@After
	public void after() throws Exception {
		cleanUp();
	}

	@Test
	public void testRoundTrip() throws Exception {
		final String entityId = KeyFactory.keyToString(123L);
		final String entityName = "TrashCanBackupDriverTest.testRoundTrip()";
		final String parentId = KeyFactory.keyToString(321L);
		trashCanDao.create(userId, entityId, entityName, parentId);
		File file = new File("TrashCanBackupDriverTest");
		assertFalse(file.exists());
		assertTrue(file.createNewFile());
		assertTrue(file.exists());
		Progress progress = new Progress();
		Set<String> idSet = new HashSet<String>();
		idSet.add(entityId);
		boolean write = trashCanBackupDriver.writeBackup(file, progress, idSet);
		assertTrue(write);
		boolean restore = trashCanBackupDriver.restoreFromBackup(file, progress);
		assertTrue(restore);
		trashCanDao.delete(userId, entityId);
		assertFalse(trashCanDao.exists(userId, entityId));
		restore = trashCanBackupDriver.restoreFromBackup(file, progress);
		assertTrue(restore);
		assertTrue(trashCanDao.exists(userId, entityId));
		assertTrue(file.delete());
	}

	private void cleanUp() throws Exception {
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userId, 0L, Long.MAX_VALUE);
		for (TrashedEntity trash : trashList) {
			trashCanDao.delete(userId, trash.getEntityId());
		}
	}
}
