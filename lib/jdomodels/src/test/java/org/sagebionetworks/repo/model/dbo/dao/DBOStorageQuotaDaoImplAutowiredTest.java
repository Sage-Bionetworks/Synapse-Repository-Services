package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.StorageQuotaAdminDao;
import org.sagebionetworks.repo.model.StorageQuotaDao;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.storage.StorageQuota;
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
		StorageQuota quota = storageQuotaDao.getQuota(userId);
		assertNull(quota);
		// Should create the quota when it does not exist yet
		quota = new StorageQuota();
		quota.setOwnerId(userId);
		quota.setQuotaInMb(3L);
		storageQuotaDao.setQuota(quota);
		quota = storageQuotaDao.getQuota(userId);
		assertNotNull(quota);
		assertNotNull(quota.getEtag());
		String oldEtag = quota.getEtag();
		assertEquals(userId, quota.getOwnerId());
		assertEquals(3, quota.getQuotaInMb().intValue());
		// Should update the quota when it already exists
		quota.setQuotaInMb(1L);
		storageQuotaDao.setQuota(quota);
		quota = storageQuotaDao.getQuota(userId);
		assertNotNull(quota);
		assertNotNull(quota.getEtag());
		assertFalse(oldEtag.endsWith(quota.getEtag()));
		assertEquals(userId, quota.getOwnerId());
		assertEquals(1, quota.getQuotaInMb().intValue());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetQuotaIllegalArgumentException1() {
		storageQuotaDao.setQuota(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetQuotaIllegalArgumentException2() {
		StorageQuota quota = new StorageQuota();
		quota.setQuotaInMb(3L);
		storageQuotaDao.setQuota(quota);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetQuotaIllegalArgumentException3() {
		StorageQuota quota = new StorageQuota();
		quota.setQuotaInMb(-3L);
		storageQuotaDao.setQuota(quota);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetQuotaIllegalArgumentException4() {
		StorageQuota quota = new StorageQuota();
		quota.setOwnerId(userId);
		storageQuotaDao.setQuota(quota);
	}

	@Test(expected=ConflictingUpdateException.class)
	public void testSetQuotaConflictingUpdateException() {
		StorageQuota quota = new StorageQuota();
		quota.setOwnerId(userId);
		quota.setQuotaInMb(1L);
		storageQuotaDao.setQuota(quota);
		quota.setQuotaInMb(3L);
		storageQuotaDao.setQuota(quota);
	}

	@Test(expected=ConflictingUpdateException.class)
	public void testSetQuotaConflictingUpdateException2() {
		StorageQuota quota = new StorageQuota();
		quota.setOwnerId(userId);
		quota.setQuotaInMb(1L);
		storageQuotaDao.setQuota(quota);
		quota = storageQuotaDao.getQuota(userId);
		quota.setEtag("etag");
		quota.setQuotaInMb(3L);
		storageQuotaDao.setQuota(quota);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetQuotaIllegalArgumentException() {
		storageQuotaDao.getQuota("");
	}
}
