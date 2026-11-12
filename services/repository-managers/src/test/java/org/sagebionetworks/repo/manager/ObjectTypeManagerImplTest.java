package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.AggregateDataConfigurationValidator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingConfig;
import org.sagebionetworks.repo.model.FacetRoundingParameters;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.DataTypeDao;

@ExtendWith(MockitoExtension.class)
public class ObjectTypeManagerImplTest {

	@Mock
	DataTypeDao mockDataTypeDao;

	@Mock
	AuthorizationManager mockAuthorizationManager;

	@Mock
	AggregateDataConfigurationValidator mockAggregateDataConfigurationValidator;

	@InjectMocks
	ObjectTypeManagerImpl manager;

	@Mock
	AuthorizationStatus mockAuthStatus;

	UserInfo userInfo;
	String objectId;
	ObjectType objectType;
	DataType dataType;
	DataTypeResponse defaultResponse;

	@BeforeEach
	public void before() {
		boolean isAdmin = false;
		Long userId = 123L;
		userInfo = new UserInfo(isAdmin, userId, AuthorizationConstants.DEFAULT_REALM_ID);
		objectId = "syn456";
		objectType = ObjectType.ENTITY;
		dataType = DataType.SENSITIVE_DATA;

		defaultResponse = new DataTypeResponse();
		defaultResponse.setObjectId(objectId);
		defaultResponse.setObjectType(objectType);
		defaultResponse.setUpdatedBy(userId.toString());
		defaultResponse.setDataType(dataType);
	}

	private AggregateDataConfiguration newAggregateConfiguration() {
		return new AggregateDataConfiguration().setSuppressionThreshold(10L)
				.setFacetPostProcessingConfig(new FacetPostProcessingConfig().setAlgorithm(FacetPostProcessingAlgorithm.ROUNDING)
						.setParameters(new FacetRoundingParameters().setRoundTo(5L)));
	}

	/**
	 * Must have the UPDATE permission to set an Object's type to SENSITIVE_DATA.
	 */
	@Test
	public void testChangeObjectsDataTypeWithSensitive() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		when(mockAuthorizationManager.canAccess(userInfo, objectId, objectType, ACCESS_TYPE.UPDATE))
				.thenReturn(mockAuthStatus);
		when(mockAuthStatus.isAuthorized()).thenReturn(true);
		when(mockDataTypeDao.changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class), nullable(AggregateDataConfiguration.class))).thenReturn(defaultResponse);

		// call under test
		DataTypeResponse result = manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);

		assertEquals(defaultResponse, result);
		verify(mockDataTypeDao).changeDataType(userInfo.getId(), objectId, objectType, dataType, null);
		verify(mockAuthorizationManager).canAccess(userInfo, objectId, objectType, ACCESS_TYPE.UPDATE);
	}

	@Test
	public void testChangeObjectsDataTypeWithSensitiveUnauthorized() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		when(mockAuthorizationManager.canAccess(userInfo, objectId, objectType, ACCESS_TYPE.UPDATE))
				.thenReturn(mockAuthStatus);
		when(mockAuthStatus.isAuthorized()).thenReturn(false);

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);
		}).getMessage();

		assertEquals("Must have UPDATE permission to change an object's DataType to : SENSITIVE_DATA", message);
		verify(mockDataTypeDao, never()).changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class), nullable(AggregateDataConfiguration.class));
	}

	/**
	 * Must be an ACT member to set an Object's type to OPEN_DATA.
	 */
	@Test
	public void testChangeObjectsDataTypeWithOpenAsACT() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockDataTypeDao.changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class), nullable(AggregateDataConfiguration.class))).thenReturn(defaultResponse);
		dataType = DataType.OPEN_DATA;

		// call under test
		DataTypeResponse result = manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);

		assertEquals(defaultResponse, result);
		verify(mockDataTypeDao).changeDataType(userInfo.getId(), objectId, objectType, dataType, null);
		verify(mockAuthorizationManager, never()).canAccess(any(UserInfo.class), any(String.class),
				any(ObjectType.class), any(ACCESS_TYPE.class));
	}

	@Test
	public void testChangeObjectsDataTypeWithOpenUnauthorized() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		dataType = DataType.OPEN_DATA;

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);
		}).getMessage();

		assertEquals(
				"Must be a member of the 'Synapse Access and Compliance Team' to change an object's DataType to: OPEN_DATA",
				message);
		verify(mockDataTypeDao, never()).changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class), nullable(AggregateDataConfiguration.class));
		verify(mockAuthorizationManager, never()).canAccess(any(UserInfo.class), any(String.class),
				any(ObjectType.class), any(ACCESS_TYPE.class));
	}

	/**
	 * An ACT member can set AGGREGATE_DATA with a valid configuration, which is
	 * forwarded to the DAO.
	 */
	@Test
	public void testChangeObjectsDataTypeWithAggregateAsACT() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockDataTypeDao.changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class), nullable(AggregateDataConfiguration.class))).thenReturn(defaultResponse);
		dataType = DataType.AGGREGATE_DATA;
		AggregateDataConfiguration config = newAggregateConfiguration();

		// call under test
		DataTypeResponse result = manager.changeObjectsDataType(userInfo, objectId, objectType, dataType, config);

		assertEquals(defaultResponse, result);
		verify(mockAggregateDataConfigurationValidator).validate(config);
		verify(mockDataTypeDao).changeDataType(userInfo.getId(), objectId, objectType, dataType, config);
		verify(mockAuthorizationManager, never()).canAccess(any(UserInfo.class), any(String.class),
				any(ObjectType.class), any(ACCESS_TYPE.class));
	}

	/**
	 * An invalid configuration (e.g. one without a facet post-processing configuration) is rejected
	 * with a 400 before anything is bound.
	 */
	@Test
	public void testChangeObjectsDataTypeWithAggregateInvalidConfig() {
		dataType = DataType.AGGREGATE_DATA;
		AggregateDataConfiguration config = newAggregateConfiguration().setFacetPostProcessingConfig(null);
		doThrow(new IllegalArgumentException("aggregateDataConfiguration.facetPostProcessingConfig is required."))
				.when(mockAggregateDataConfigurationValidator).validate(config);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.changeObjectsDataType(userInfo, objectId, objectType, dataType, config);
		}).getMessage();

		assertEquals("aggregateDataConfiguration.facetPostProcessingConfig is required.", message);
		verify(mockDataTypeDao, never()).changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class), nullable(AggregateDataConfiguration.class));
		verifyNoInteractions(mockAuthorizationManager);
	}

	/**
	 * A non-ACT member cannot set AGGREGATE_DATA even with the UPDATE permission.
	 */
	@Test
	public void testChangeObjectsDataTypeWithAggregateNonACT() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		dataType = DataType.AGGREGATE_DATA;
		AggregateDataConfiguration config = newAggregateConfiguration();

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.changeObjectsDataType(userInfo, objectId, objectType, dataType, config);
		}).getMessage();

		assertEquals(
				"Must be a member of the 'Synapse Access and Compliance Team' to change an object's DataType to: AGGREGATE_DATA",
				message);
		verify(mockDataTypeDao, never()).changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class), nullable(AggregateDataConfiguration.class));
		verify(mockAuthorizationManager, never()).canAccess(any(UserInfo.class), any(String.class),
				any(ObjectType.class), any(ACCESS_TYPE.class));
	}

	@Test
	public void testChangeObjectsDataTypeWithNonAggregateAndConfig() {
		dataType = DataType.SENSITIVE_DATA;
		// a configuration is not allowed for a non-aggregate type
		AggregateDataConfiguration config = newAggregateConfiguration();

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.changeObjectsDataType(userInfo, objectId, objectType, dataType, config);
		}).getMessage();

		assertEquals("An aggregateDataConfiguration can only be provided for the AGGREGATE_DATA DataType.", message);
		verify(mockDataTypeDao, never()).changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class), nullable(AggregateDataConfiguration.class));
		verifyNoInteractions(mockAggregateDataConfigurationValidator);
	}

	/**
	 * The 4-arg overload must delegate to the DAO with a null configuration.
	 */
	@Test
	public void testChangeObjectsDataTypeFourArgDelegatesWithNullConfig() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockDataTypeDao.changeDataType(any(Long.class), any(String.class), any(ObjectType.class),
				any(DataType.class), nullable(AggregateDataConfiguration.class))).thenReturn(defaultResponse);
		dataType = DataType.OPEN_DATA;

		// call under test
		manager.changeObjectsDataType(userInfo, objectId, objectType, dataType);

		verify(mockDataTypeDao).changeDataType(userInfo.getId(), objectId, objectType, dataType, null);
	}

}
