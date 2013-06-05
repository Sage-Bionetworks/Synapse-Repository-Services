package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.StorageQuotaAdminDao;
import org.sagebionetworks.repo.model.StorageQuotaDao;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOStorageQuotaDaoImplAutowiredTest {

	@Autowired
	private StorageQuotaDao storageQuotaDao;

	@Autowired
	private StorageQuotaAdminDao storageQuotaAdminDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	private String userId;

	@Before
	public void before() {
		assertNotNull(storageQuotaDao);
		assertNotNull(storageQuotaAdminDao);
		assertNotNull(userGroupDAO);
		userId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(userId);
	}

	@After
	public void after() {
		storageQuotaAdminDao.clear();
	}

	@Test
	public void test() {
		// Should return null when quota does not exist
		Integer quota = storageQuotaDao.getQuota(userId);
		assertNull(quota);
		// Should create the quota when it does not exist yet in the db
		storageQuotaDao.setQuota(userId, 3);
		quota = storageQuotaDao.getQuota(userId);
		assertEquals(3, quota.intValue());
		// Should update the quota when it already exists in the db
		storageQuotaDao.setQuota(userId, 1);
		quota = storageQuotaDao.getQuota(userId);
		assertEquals(1, quota.intValue());
	}
}
