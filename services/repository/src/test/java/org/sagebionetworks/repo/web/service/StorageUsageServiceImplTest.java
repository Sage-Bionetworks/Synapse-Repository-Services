package org.sagebionetworks.repo.web.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.StorageUsageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class StorageUsageServiceImplTest {

	private final String username = "foo";
	private final String userId = "1234";
	private final String adminUsername = "bar";
	private final String adminUserId = "5678";
	private final List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>(0);
	private final StorageUsageSummaryList susList = Mockito.mock(StorageUsageSummaryList.class);
	private final StorageUsageService suService = new StorageUsageServiceImpl();
	private final Integer offset = Integer.valueOf(0);
	private final Integer limit = Integer.valueOf(1);
	private final String nodeId = "nodeId";

	@Before
	public void before() throws Exception {

		UserGroup userGroup = Mockito.mock(UserGroup.class);
		Mockito.when(userGroup.getId()).thenReturn("0");

		UserInfo userInfo = Mockito.mock(UserInfo.class);
		Mockito.when(userInfo.isAdmin()).thenReturn(false);
		Mockito.when(userInfo.getIndividualGroup()).thenReturn(userGroup);

		UserInfo adminUserInfo = Mockito.mock(UserInfo.class);
		Mockito.when(adminUserInfo.isAdmin()).thenReturn(true);
		Mockito.when(adminUserInfo.getIndividualGroup()).thenReturn(userGroup);

		UserManager userMan = Mockito.mock(UserManager.class);
		Mockito.when(userMan.getUserInfo(username)).thenReturn(userInfo);
		Mockito.when(userMan.getUserInfo(adminUsername)).thenReturn(adminUserInfo);
		Mockito.when(userMan.getGroupName(userId)).thenReturn(username);
		Mockito.when(userMan.getGroupName(adminUserId)).thenReturn(adminUsername);

		AuthorizationManager auMan = Mockito.mock(AuthorizationManager.class);
		Mockito.when(auMan.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);

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
		ReflectionTestUtils.setField(srv, "authorizationManager", auMan);
		ReflectionTestUtils.setField(srv, "storageUsageManager", suMan);
	}

	@Test
	public void testGetUsageAdminUser() throws Exception {
		Assert.assertNotNull(suService.getUsage(adminUsername, dList));
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetUsageNonAdminUser() throws Exception {
		suService.getUsage(username, dList);
		Assert.fail();
	}

	@Test
	public void testGetUsageForUserSameUser() throws Exception {
		StorageUsageSummaryList results = suService.getUsageForUser(username, userId, dList);
		Assert.assertNotNull(results);
	}

	@Test
	public void testGetUsageForUserAdminUser() throws Exception {
		StorageUsageSummaryList results = suService.getUsageForUser(adminUsername, userId, dList);
		Assert.assertNotNull(results);
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetUsageForUserNonAdminUser() throws Exception {
		suService.getUsageForUser(username, adminUserId, dList);
		Assert.fail();
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetUsageByUserInRangeNonAdmin() throws Exception {
		suService.getUsageByUserInRange(username, offset, limit);
		Assert.fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetUsageForUser() throws Exception {
		List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>();
		dList.add(StorageUsageDimension.CONTENT_TYPE);
		dList.add(StorageUsageDimension.USER_ID); // IllegalArgumentException
		suService.getUsageForUser(adminUsername, userId, dList);
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetUsageInRangeForUser() throws Exception {
		suService.getUsageInRangeForUser(username, adminUsername, 0, 10, "");
	}
}
