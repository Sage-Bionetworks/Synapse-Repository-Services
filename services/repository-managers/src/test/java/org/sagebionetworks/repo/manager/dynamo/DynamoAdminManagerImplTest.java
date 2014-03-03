package org.sagebionetworks.repo.manager.dynamo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

public class DynamoAdminManagerImplTest {

	private Long adminUserId = 2938475L;
	private Long nonAdminUserId = 87435L; 
	
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
		when(userManager.getUserInfo(adminUserId)).thenReturn(adminUserInfo);
		when(userManager.getUserInfo(nonAdminUserId)).thenReturn(nonAdminUserInfo);

		// Inject the mocks
		DynamoAdminManagerImpl dynamoAdminManager = new DynamoAdminManagerImpl();
		ReflectionTestUtils.setField(dynamoAdminManager, "dynamoAdminDao", dynamoAdminDao);
		ReflectionTestUtils.setField(dynamoAdminManager, "userManager", userManager);

		// Run the tests
		final String tableName = "table name";
		final String hashKeyName = "hash key name";
		final String rangeKeyName = "range key name";
		dynamoAdminManager.clear(adminUserId, tableName, hashKeyName, rangeKeyName);
		verify(dynamoAdminDao, times(1)).clear(tableName, hashKeyName, rangeKeyName);
		try {
			dynamoAdminManager.clear(nonAdminUserId, tableName, hashKeyName, rangeKeyName);
		} catch (UnauthorizedException e) {
			Assert.assertTrue(true);
		}
		verify(dynamoAdminDao, times(1)).clear(tableName, hashKeyName, rangeKeyName);
	}
}
