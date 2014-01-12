package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.sagebionetworks.repo.model.StorageQuotaDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.storage.StorageQuota;
import org.springframework.test.util.ReflectionTestUtils;

public class StorageQuotaManagerImplTest {

	@Test
	public void testGetQuota() {

		UserInfo currUser = new UserInfo(false, 1L);
		UserInfo user = new UserInfo(false, 1L);

		StorageQuotaDao quotaDao = mock(StorageQuotaDao.class);
		StorageQuota quota = new StorageQuota();
		quota.setQuotaInMb(3L);
		when(quotaDao.getQuota("1")).thenReturn(quota);
		StorageQuotaManager manager = new StorageQuotaManagerImpl();
		ReflectionTestUtils.setField(manager, "storageQuotaDao", quotaDao);
		int quotaInMb = manager.getQuotaForUser(currUser, user);
		assertEquals(3, quotaInMb);
	}

	@Test
	public void testGetDefaultQuota() {

		UserInfo currUser = new UserInfo(false, 1L);
		UserInfo user = new UserInfo(false, 1L);

		StorageQuotaDao quotaDao = mock(StorageQuotaDao.class);
		when(quotaDao.getQuota("1")).thenReturn((StorageQuota)null);
		StorageQuotaManager manager = new StorageQuotaManagerImpl();
		ReflectionTestUtils.setField(manager, "storageQuotaDao", quotaDao);
		int quota = manager.getQuotaForUser(currUser, user);
		assertEquals(2000, quota);
	}

	@Test
	public void testSetQuota() {

		UserInfo currUser = new UserInfo(true, 1L);
		UserInfo user = new UserInfo(false, 1L);

		StorageQuotaDao quotaDao = mock(StorageQuotaDao.class);
		StorageQuotaManager manager = new StorageQuotaManagerImpl();
		ReflectionTestUtils.setField(manager, "storageQuotaDao", quotaDao);
		manager.setQuotaForUser(currUser, user, 1);
		verify(quotaDao, times(1)).setQuota(any(StorageQuota.class));
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetQuotaUnauthorizedException1() {
		UserInfo currentUser = mock(UserInfo.class);
		when(currentUser.isAdmin()).thenReturn(false);
		UserInfo user = mock(UserInfo.class);
		StorageQuotaManager manager = new StorageQuotaManagerImpl();
		manager.setQuotaForUser(currentUser, user, 0);
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetQuotaUnauthorizedException2() {
		UserInfo currUser = new UserInfo(false, 1L);
		UserInfo user = new UserInfo(false, 2L);
		StorageQuotaManager manager = new StorageQuotaManagerImpl();
		manager.getQuotaForUser(currUser, user);
	}

	@Test(expected=UnauthorizedException.class)
	public void testSetQuotaUnauthorizedException() {
		UserInfo currentUser = mock(UserInfo.class);
		when(currentUser.isAdmin()).thenReturn(false);
		UserInfo user = mock(UserInfo.class);
		StorageQuotaManager manager = new StorageQuotaManagerImpl();
		manager.setQuotaForUser(currentUser, user, 0);
	}
}
