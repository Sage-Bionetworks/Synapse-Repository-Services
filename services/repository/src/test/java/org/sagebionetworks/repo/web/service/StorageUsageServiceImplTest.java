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

	private final Long userId = 1234L;
	private final Long adminUserId = 5678L;
	private final List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>(0);
	private final StorageUsageSummaryList susList = Mockito.mock(StorageUsageSummaryList.class);
	private final StorageUsageService suService = new StorageUsageServiceImpl();
	private final Integer offset = Integer.valueOf(0);
	private final Integer limit = Integer.valueOf(1);

	@Before
	public void before() throws Exception {

		UserInfo userInfo = new UserInfo(false, 0L);
		UserInfo adminUserInfo = new UserInfo(true, 1L);

		UserManager userMan = Mockito.mock(UserManager.class);
		Mockito.when(userMan.getUserInfo(userId)).thenReturn(userInfo);
		Mockito.when(userMan.getUserInfo(adminUserId)).thenReturn(adminUserInfo);

		StorageUsageManager suMan = Mockito.mock(StorageUsageManager.class);
		Mockito.when(suMan.getUsage(dList)).thenReturn(susList);
		Mockito.when(suMan.getUsageForUser(userId, dList)).thenReturn(susList);
		Mockito.when(suMan.getUsageForUser(adminUserId, dList)).thenReturn(susList);
		Mockito.when(suMan.getUsageByUserInRange(offset, limit)).thenReturn(susList);

		StorageUsageService srv = suService;
		if(AopUtils.isAopProxy(srv) && srv instanceof Advised) {
			Object target = ((Advised)srv).getTargetSource().getTarget();
			srv = (StorageUsageService)target;
		}
		ReflectionTestUtils.setField(srv, "userManager", userMan);
		ReflectionTestUtils.setField(srv, "storageUsageManager", suMan);
	}

	@Test
	public void testGetUsageAdminUser() throws Exception {
		Assert.assertNotNull(suService.getUsage(adminUserId, dList));
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetUsageNonAdminUser() throws Exception {
		suService.getUsage(userId, dList);
		Assert.fail();
	}

	@Test
	public void testGetUsageForUserSameUser() throws Exception {
		StorageUsageSummaryList results = suService.getUsageForUser(userId, userId, dList);
		Assert.assertNotNull(results);
	}

	@Test
	public void testGetUsageForUserAdminUser() throws Exception {
		StorageUsageSummaryList results = suService.getUsageForUser(adminUserId, userId, dList);
		Assert.assertNotNull(results);
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetUsageForUserNonAdminUser() throws Exception {
		suService.getUsageForUser(userId, adminUserId, dList);
		Assert.fail();
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetUsageByUserInRangeNonAdmin() throws Exception {
		suService.getUsageByUserInRange(userId, offset, limit);
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetUsageForUser() throws Exception {
		List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>();
		dList.add(StorageUsageDimension.CONTENT_TYPE);
		dList.add(StorageUsageDimension.USER_ID); // IllegalArgumentException
		suService.getUsageForUser(adminUserId, userId, dList);
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetUsageInRangeForUser() throws Exception {
		suService.getUsageInRangeForUser(userId, adminUserId, 0, 10, "");
	}
}
