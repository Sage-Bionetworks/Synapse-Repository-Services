package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
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

	@Before
	public void before() {
		dataTypeDao.truncateAllData();
		// create a user
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		ug.setCreationDate(new Date());
		userId = userGroupDAO.create(ug);
		userIdTwo = userGroupDAO.create(ug);

		objectId = "syn123";
		objectType = ObjectType.ENTITY;
		dataType = DataType.OPEN_DATA;
	}

	@After
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
		assertNotNull(one.getUpdatedOn().getTime() < two.getUpdatedOn().getTime());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testChangeDataTypeNullUserId() {
		userId = null;
		// call under test
		dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testChangeDataTypeNullObjectId() {
		objectId = null;
		// call under test
		dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testChangeDataTypeNullObjectType() {
		objectType = null;
		// call under test
		dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testChangeDataTypeNullDataType() {
		dataType = null;
		// call under test
		dataTypeDao.changeDataType(userId, objectId, objectType, dataType);
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
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetObjectDataTypeNullObjectId() {
		objectId = null;
		// call under test
		dataTypeDao.getObjectDataType(objectId, objectType);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetObjectDataTypeNullObjectType() {
		objectType = null;
		// call under test
		dataTypeDao.getObjectDataType(objectId, objectType);
	}


}
