package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DataTypeDao;

@RunWith(MockitoJUnitRunner.class)
public class ObjectTypeManagerImplTest {

	@Mock
	DataTypeDao mockDataTypeDao;

	@Mock
	AuthorizationManager mockAuthorizationManager;

	@InjectMocks
	ObjectTypeManagerImpl manager;

	UserInfo userInfo;
	String objectId;
	ObjectType objectType;
	DataType dataType;
	@Mock
	AuthorizationStatus mockAuthStatus;
	DataTypeResponse defaultResponse;

	@Before
	public void before() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(any(UserInfo.class))).thenReturn(false);
		boolean isAuthorized = true;
		when(mockAuthStatus.isAuthorized()).thenReturn(isAuthorized);
		when(mockAuthorizationManager.canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class),
				any(ACCESS_TYPE.class))).thenReturn(mockAuthStatus);

		boolean isAdmin = false;
		Long userId = 123L;
		userInfo = new UserInfo(isAdmin, userId);
		objectId = "syn456";
		objectType = ObjectType.ENTITY;
		dataType = DataType.SENSITIVE_DATA;

		defaultResponse = new DataTypeResponse();
		defaultResponse.setObjectId(objectId);
		defaultResponse.setObjectType(objectType);
		defaultResponse.setUpdatedBy(userId.toString());
		defaultResponse.setDataType(dataType);
		when(mockDataTypeDao.changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class))).thenReturn(defaultResponse);
	}

	/**
	 * Must have update the UPDATE permission to set an Object's type to
	 * DataType.SENSITIVE_DATA
	 */
	@Test
	public void testChangeObjectsDataTypeSensitive() {
		// call under test
		DataTypeResponse returnedResponse = manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);
		assertEquals(defaultResponse, returnedResponse);
		verify(mockDataTypeDao).changeDataType(userInfo.getId(), objectId, objectType, dataType);
		verify(mockAuthorizationManager).canAccess(userInfo, objectId, objectType, ACCESS_TYPE.UPDATE);
		verify(mockAuthorizationManager).isACTTeamMemberOrAdmin(any(UserInfo.class));
	}

	@Test
	public void testChangeObjectsDataTypeSensitiveUnauthroized() {
		boolean isAuthorized = false;
		when(mockAuthStatus.isAuthorized()).thenReturn(isAuthorized);
		try {
			// call under test
			manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);
			fail();
		} catch (UnauthorizedException e) {
			// expected;
		}
		verify(mockDataTypeDao, never()).changeDataType(any(Long.class), any(String.class), any(ObjectType.class), any(DataType.class));
		verify(mockAuthorizationManager).canAccess(userInfo, objectId, objectType, ACCESS_TYPE.UPDATE);
		verify(mockAuthorizationManager).isACTTeamMemberOrAdmin(any(UserInfo.class));
	}
	
	/**
	 * Must be an ACT member to set an Object's type to DataType.OPEN_DATA
	 */
	@Test
	public void testChangeObjectsDataTypeOpenAsACT() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(any(UserInfo.class))).thenReturn(true);
		dataType = DataType.OPEN_DATA;
		// call under test
		DataTypeResponse returnedResponse = manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);
		assertEquals(defaultResponse, returnedResponse);
		verify(mockDataTypeDao).changeDataType(userInfo.getId(), objectId, objectType, dataType);
		verify(mockAuthorizationManager, never()).canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class));
		verify(mockAuthorizationManager).isACTTeamMemberOrAdmin(userInfo);
	}
	
	/**
	 * Must be an ACT member to set an Object's type to DataType.OPEN_DATA
	 */
	@Test
	public void testChangeObjectsDataTypeSensitiveAsACT() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(any(UserInfo.class))).thenReturn(true);
		dataType = DataType.SENSITIVE_DATA;
		// call under test
		DataTypeResponse returnedResponse = manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);
		assertEquals(defaultResponse, returnedResponse);
		verify(mockDataTypeDao).changeDataType(userInfo.getId(), objectId, objectType, dataType);
		verify(mockAuthorizationManager, never()).canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class));
		verify(mockAuthorizationManager).isACTTeamMemberOrAdmin(userInfo);
	}
	
	@Test
	public void testChangeObjectsDataTypeOpenUnauthroized() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(any(UserInfo.class))).thenReturn(false);
		dataType = DataType.OPEN_DATA;
		try {
			// call under test
			manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);
			fail();
		} catch (UnauthorizedException e) {
			// expected;
		}
		verify(mockDataTypeDao, never()).changeDataType(any(Long.class), any(String.class), any(ObjectType.class), any(DataType.class));
		verify(mockAuthorizationManager, never()).canAccess(any(UserInfo.class), any(String.class), any(ObjectType.class), any(ACCESS_TYPE.class));
		verify(mockAuthorizationManager).isACTTeamMemberOrAdmin(userInfo);
	}

}
