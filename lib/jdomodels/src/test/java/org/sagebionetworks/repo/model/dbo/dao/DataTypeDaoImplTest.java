package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingConfig;
import org.sagebionetworks.repo.model.FacetPostProcessingParameters;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DataTypeDaoImplTest {

	@Autowired
	DataTypeDao dataTypeDao;

	@Autowired
	UserGroupDAO userGroupDAO;

	Long userId;
	Long userIdTwo;

	String objectId;
	ObjectType objectType;
	DataType dataType;

	@BeforeEach
	public void before() {
		dataTypeDao.truncateAllData();
		// create a user
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		ug.setCreationDate(new Date());
		ug.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		userId = userGroupDAO.create(ug);
		userIdTwo = userGroupDAO.create(ug);

		objectId = "syn123";
		objectType = ObjectType.ENTITY;
		dataType = DataType.OPEN_DATA;
	}

	@AfterEach
	public void after() {
		dataTypeDao.truncateAllData();
	}

	@Test
	public void testCreate() {
		// call under test
		DataTypeResponse response = dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
		assertNotNull(response);
		assertEquals(objectId, response.getObjectId());
		assertEquals(objectType, response.getObjectType());
		assertEquals(dataType, response.getDataType());
		assertEquals(userId.toString(), response.getUpdatedBy());
		assertNotNull(response.getUpdatedOn());
	}

	@Test
	public void testUpdate() throws InterruptedException {
		// call under test
		DataTypeResponse one = dataTypeDao.changeDataType(userId, objectId, objectType, DataType.OPEN_DATA);
		assertNotNull(one);
		assertEquals(DataType.OPEN_DATA, one.getDataType());
		assertEquals(userId.toString(), one.getUpdatedBy());
		assertNotNull(one.getUpdatedOn());
		// sleep to change updated on
		Thread.sleep(10L);
		// change it again
		DataTypeResponse two = dataTypeDao.changeDataType(userIdTwo, objectId, objectType, DataType.SENSITIVE_DATA);
		assertNotNull(two);
		assertEquals(DataType.SENSITIVE_DATA, two.getDataType());
		assertEquals(userIdTwo.toString(), two.getUpdatedBy());
		assertTrue(one.getUpdatedOn().getTime() < two.getUpdatedOn().getTime());
	}

	@Test
	public void testChangeDataTypeNullUserId() {
		userId = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
		});
	}

	@Test
	public void testChangeDataTypeNullObjectId() {
		objectId = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
		});
	}

	@Test
	public void testChangeDataTypeNullObjectType() {
		objectType = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
		});
	}

	@Test
	public void testChangeDataTypeNullDataType() {
		dataType = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
		});
	}

	@Test
	public void testGetObjectDataType() {
		// setup a type.
		dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
		// call under test
		DataType resultType = dataTypeDao.getObjectDataType(objectId, objectType);
		assertEquals(dataType, resultType);
	}

	@Test
	public void testGetObjectDataTypeDoesNotExist() {
		// call under test
		DataType resultType = dataTypeDao.getObjectDataType(objectId, objectType);
		assertEquals(DataTypeDaoImpl.DEFAULT_DATA_TYPE, resultType);
	}

	@Test
	public void testGetObjectDataTypeNullObjectId() {
		objectId = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			dataTypeDao.getObjectDataType(objectId, objectType);
		});
	}

	@Test
	public void testGetObjectDataTypeNullObjectType() {
		objectType = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			dataTypeDao.getObjectDataType(objectId, objectType);
		});
	}

	private AggregateDataConfiguration newAggregateConfiguration() {
		return new AggregateDataConfiguration().setSuppressionThreshold(10L).setFacetPostProcessingConfig(
				new FacetPostProcessingConfig().setAlgorithm(FacetPostProcessingAlgorithm.ROUNDING)
						.setParameters(new FacetPostProcessingParameters()));
	}

	@Test
	public void testChangeDataTypeWithAggregateData() {
		AggregateDataConfiguration config = newAggregateConfiguration();
		// call under test
		DataTypeResponse response = dataTypeDao.changeDataType(userId, objectId, objectType, DataType.AGGREGATE_DATA,
				config);
		assertNotNull(response);
		assertEquals(DataType.AGGREGATE_DATA, response.getDataType());
		// the whole configuration must round-trip through the JSON column
		assertEquals(config, response.getAggregateDataConfiguration());
	}

	@Test
	public void testGetAggregateDataConfigurationWithAggregateData() {
		AggregateDataConfiguration config = newAggregateConfiguration();
		dataTypeDao.changeDataType(userId, objectId, objectType, DataType.AGGREGATE_DATA, config);
		// call under test
		Optional<AggregateDataConfiguration> result = dataTypeDao.getAggregateDataConfiguration(objectId, objectType);
		assertTrue(result.isPresent());
		assertEquals(config, result.get());
	}

	@Test
	public void testChangeDataTypeFromAggregateClearsConfiguration() {
		dataTypeDao.changeDataType(userId, objectId, objectType, DataType.AGGREGATE_DATA, newAggregateConfiguration());
		// call under test: switch back to a non-aggregate type
		DataTypeResponse response = dataTypeDao.changeDataType(userId, objectId, objectType, DataType.SENSITIVE_DATA);
		assertNull(response.getAggregateDataConfiguration());
		assertFalse(dataTypeDao.getAggregateDataConfiguration(objectId, objectType).isPresent());
	}

	@Test
	public void testGetAggregateDataConfigurationWithNonAggregateDecoy() {
		// a non-aggregate row must not report a configuration
		dataTypeDao.changeDataType(userId, objectId, objectType, DataType.OPEN_DATA);
		// call under test
		Optional<AggregateDataConfiguration> result = dataTypeDao.getAggregateDataConfiguration(objectId, objectType);
		assertFalse(result.isPresent());
	}

	@Test
	public void testGetAggregateDataConfigurationDoesNotExist() {
		// call under test: no row for this object at all
		Optional<AggregateDataConfiguration> result = dataTypeDao.getAggregateDataConfiguration(objectId, objectType);
		assertFalse(result.isPresent());
	}

	@Test
	public void testGetAggregateDataConfigurationNullObjectId() {
		objectId = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			dataTypeDao.getAggregateDataConfiguration(objectId, objectType);
		});
	}

	@Test
	public void testGetAggregateDataConfigurationNullObjectType() {
		objectType = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			dataTypeDao.getAggregateDataConfiguration(objectId, objectType);
		});
	}

}
