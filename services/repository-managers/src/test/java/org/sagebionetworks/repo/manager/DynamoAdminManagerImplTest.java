package org.sagebionetworks.repo.manager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

public class DynamoAdminManagerImplTest {

	@Test
	public void testClear() throws Exception {

		// Mock the dynamo admin dao
		DynamoAdminDao dynamoAdminDao = mock(DynamoAdminDao.class);

		// Mock the user manager
		UserInfo adminUserInfo = mock(UserInfo.class);
		when(adminUserInfo.isAdmin()).thenReturn(true);
		UserInfo nonAdminUserInfo = mock(UserInfo.class);
		when(nonAdminUserInfo.isAdmin()).thenReturn(false);
		UserManager userManager = mock(UserManager.class);
		final String adminUserName = "admin";
		when(userManager.getUserInfo(adminUserName)).thenReturn(adminUserInfo);
		final String nonAdminUserName = "nonAdmin";
		when(userManager.getUserInfo(nonAdminUserName)).thenReturn(nonAdminUserInfo);

		// Inject the mocks
		DynamoAdminManagerImpl dynamoAdminManager = new DynamoAdminManagerImpl();
		ReflectionTestUtils.setField(dynamoAdminManager, "dynamoAdminDao", dynamoAdminDao);
		ReflectionTestUtils.setField(dynamoAdminManager, "userManager", userManager);

		// Run the tests
		final String tableName = "table name";
		final String hashKeyName = "hash key name";
		final String rangeKeyName = "range key name";
		dynamoAdminManager.clear(adminUserName, tableName, hashKeyName, rangeKeyName);
		verify(dynamoAdminDao, times(1)).clear(tableName, hashKeyName, rangeKeyName);
		try {
			dynamoAdminManager.clear(nonAdminUserName, tableName, hashKeyName, rangeKeyName);
		} catch (UnauthorizedException e) {
			Assert.assertTrue(true);
		}
		verify(dynamoAdminDao, times(1)).clear(tableName, hashKeyName, rangeKeyName);
	}
}
