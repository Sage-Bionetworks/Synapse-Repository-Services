package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.sagebionetworks.repo.model.StorageQuotaDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

public class StorageQuotaManagerImplTest {

	@Test
	public void testGetQuota() {

		UserInfo currUser = mock(UserInfo.class);
		when(currUser.isAdmin()).thenReturn(false);
		UserGroup currUserGroup = mock(UserGroup.class);
		when(currUserGroup.getId()).thenReturn("1");
		when(currUser.getIndividualGroup()).thenReturn(currUserGroup);
		UserInfo user = mock(UserInfo.class);
		UserGroup userGroup = mock(UserGroup.class);
		when(userGroup.getId()).thenReturn("1");
		when(user.getIndividualGroup()).thenReturn(userGroup);

		StorageQuotaDao quotaDao = mock(StorageQuotaDao.class);
		when(quotaDao.getQuota("1")).thenReturn(3);
		StorageQuotaManager manager = new StorageQuotaManagerImpl();
		ReflectionTestUtils.setField(manager, "storageQuotaDao", quotaDao);
		int quota = manager.getQuotaForUser(currUser, user);
		assertEquals(3, quota);
	}

	@Test
	public void testGetDefaultQuota() {

		UserInfo currUser = mock(UserInfo.class);
		when(currUser.isAdmin()).thenReturn(false);
		UserGroup currUserGroup = mock(UserGroup.class);
		when(currUserGroup.getId()).thenReturn("1");
		when(currUser.getIndividualGroup()).thenReturn(currUserGroup);
		UserInfo user = mock(UserInfo.class);
		UserGroup userGroup = mock(UserGroup.class);
		when(userGroup.getId()).thenReturn("1");
		when(user.getIndividualGroup()).thenReturn(userGroup);

		StorageQuotaDao quotaDao = mock(StorageQuotaDao.class);
		when(quotaDao.getQuota("1")).thenReturn((Integer)null);
		StorageQuotaManager manager = new StorageQuotaManagerImpl();
		ReflectionTestUtils.setField(manager, "storageQuotaDao", quotaDao);
		int quota = manager.getQuotaForUser(currUser, user);
		assertEquals(2000, quota);
	}

	@Test
	public void testSetQuota() {

		UserInfo currUser = mock(UserInfo.class);
		when(currUser.isAdmin()).thenReturn(true);
		UserGroup currUserGroup = mock(UserGroup.class);
		when(currUserGroup.getId()).thenReturn("1");
		when(currUser.getIndividualGroup()).thenReturn(currUserGroup);
		UserInfo user = mock(UserInfo.class);
		UserGroup userGroup = mock(UserGroup.class);
		when(userGroup.getId()).thenReturn("1");
		when(user.getIndividualGroup()).thenReturn(userGroup);

		StorageQuotaDao quotaDao = mock(StorageQuotaDao.class);
		StorageQuotaManager manager = new StorageQuotaManagerImpl();
		ReflectionTestUtils.setField(manager, "storageQuotaDao", quotaDao);
		manager.setQuotaForUser(currUser, user, 1);
		verify(quotaDao, times(1)).setQuota("1", 1);
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
		UserInfo currUser = mock(UserInfo.class);
		when(currUser.isAdmin()).thenReturn(false);
		UserGroup currUserGroup = mock(UserGroup.class);
		when(currUserGroup.getId()).thenReturn("1");
		when(currUser.getIndividualGroup()).thenReturn(currUserGroup);
		UserInfo user = mock(UserInfo.class);
		UserGroup userGroup = mock(UserGroup.class);
		when(userGroup.getId()).thenReturn("2");
		when(user.getIndividualGroup()).thenReturn(userGroup);
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
