package org.sagebionetworks.repo.web.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.StorageUsageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class StorageUsageServiceImplTest {

	private final String userId = "0";
	private final String adminUserId = "1";
	private final List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>(0);
	private final StorageUsageSummaryList susList = Mockito.mock(StorageUsageSummaryList.class);
	private final StorageUsageService suService = new StorageUsageServiceImpl();

	@Before
	public void before() throws Exception {

		UserInfo userInfo = Mockito.mock(UserInfo.class);
		Mockito.when(userInfo.isAdmin()).thenReturn(false);

		UserInfo adminUserInfo = Mockito.mock(UserInfo.class);
		Mockito.when(adminUserInfo.isAdmin()).thenReturn(true);

		UserManager userMan = Mockito.mock(UserManager.class);
		Mockito.when(userMan.getUserInfo(userId)).thenReturn(userInfo);
		Mockito.when(userMan.getUserInfo(adminUserId)).thenReturn(adminUserInfo);

		StorageUsageManager suMan = Mockito.mock(StorageUsageManager.class);
		Mockito.when(suMan.getStorageUsage(userId, dList)).thenReturn(susList);
		Mockito.when(suMan.getStorageUsage(adminUserId, dList)).thenReturn(susList);

		StorageUsageService srv = suService;
		if(AopUtils.isAopProxy(srv) && srv instanceof Advised) {
			Object target = ((Advised)srv).getTargetSource().getTarget();
			srv = (StorageUsageService)target;
		}
		ReflectionTestUtils.setField(srv, "userManager", userMan);
		ReflectionTestUtils.setField(srv, "storageUsageManager", suMan);
	}

	@Test
	public void testSameUser() {
		StorageUsageSummaryList results = suService.getStorageUsage(userId, userId, dList);
		Assert.assertNotNull(results);
		Assert.assertEquals(susList, results);
	}

	@Test
	public void testAdminUser() {
		StorageUsageSummaryList results = suService.getStorageUsage(adminUserId, userId, dList);
		Assert.assertNotNull(results);
		Assert.assertEquals(susList, results);
	}

	@Test(expected=UnauthorizedException.class)
	public void testNonAdminUser() {
		suService.getStorageUsage(userId, adminUserId, dList);
		Assert.fail();
	}
}
