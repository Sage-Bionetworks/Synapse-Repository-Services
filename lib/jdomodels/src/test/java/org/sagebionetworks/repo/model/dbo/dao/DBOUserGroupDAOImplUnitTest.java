package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

public class DBOUserGroupDAOImplUnitTest {
	
	private TransactionalMessenger mockTransactionalMessenger;
	private IdGenerator mockIdGenerator;
	private DBOBasicDao mockBasicDAO;
	private SimpleJdbcTemplate mockSimpleJdbcTemplate;
	private DBOUserGroupDAOImpl userGroupDAO;
	private UserGroup ug;
	private Long id = 1L;
	

	@Before
	public void setup() {
		mockTransactionalMessenger = Mockito.mock(TransactionalMessenger.class);
		mockIdGenerator = Mockito.mock(IdGenerator.class);
		mockBasicDAO = Mockito.mock(DBOBasicDao.class);
		mockSimpleJdbcTemplate = Mockito.mock(SimpleJdbcTemplate.class);
		userGroupDAO = new DBOUserGroupDAOImpl();
		ReflectionTestUtils.setField(userGroupDAO, "basicDao", mockBasicDAO);
		ReflectionTestUtils.setField(userGroupDAO, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(userGroupDAO, "transactionalMessenger", mockTransactionalMessenger);
		ReflectionTestUtils.setField(userGroupDAO, "simpleJdbcTemplate", mockSimpleJdbcTemplate);
		
		ug = new UserGroup();
		ug.setCreationDate(new Date());
		ug.setEtag("etag");
		ug.setId(id.toString());
		ug.setIsIndividual(false);
		
		Mockito.when(mockIdGenerator.generateNewId(TYPE.PRINCIPAL_ID)).thenReturn(id);
		doAnswer(new Answer<DBOUserGroup>(){
			@Override
			public DBOUserGroup answer(InvocationOnMock invocation) throws Throwable {
				return (DBOUserGroup) invocation.getArguments()[0];
			}}).when(mockBasicDAO).createNew(any(DBOUserGroup.class));
		DBOUserGroup dbo = new DBOUserGroup();
		dbo.setId(Long.parseLong(ug.getId()));
		dbo.setEtag(ug.getEtag());
		UserGroupUtils.copyDtoToDbo(ug, dbo);
		when(mockSimpleJdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(SqlParameterSource.class))).thenReturn(dbo);
		when(mockBasicDAO.update(any(DatabaseObject.class))).thenReturn(true);
	}
	
	@Test
	public void createSendMessageTest() {
		userGroupDAO.create(ug);
		ArgumentCaptor<Long> idCapture = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<ObjectType> objectTypeCapture = ArgumentCaptor.forClass(ObjectType.class);
		ArgumentCaptor<String> etagCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ChangeType> typeCapture = ArgumentCaptor.forClass(ChangeType.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(idCapture.capture().toString(), objectTypeCapture.capture(), etagCapture.capture(), typeCapture.capture());
		assertEquals(idCapture.getValue(), id.toString());
		assertEquals(ObjectType.PRINCIPAL, objectTypeCapture.getValue());
		assertNotNull(etagCapture.getValue());
		assertEquals(ChangeType.CREATE, typeCapture.getValue());
	}
	
	@Test
	public void updateSendMessageTest() {
		userGroupDAO.update(ug);
		ArgumentCaptor<Long> idCapture = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<ObjectType> objectTypeCapture = ArgumentCaptor.forClass(ObjectType.class);
		ArgumentCaptor<String> etagCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ChangeType> typeCapture = ArgumentCaptor.forClass(ChangeType.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(idCapture.capture().toString(), objectTypeCapture.capture(), etagCapture.capture(), typeCapture.capture());
		assertEquals(idCapture.getValue(), id.toString());
		assertEquals(ObjectType.PRINCIPAL, objectTypeCapture.getValue());
		assertNotNull(etagCapture.getValue());
		assertEquals(ChangeType.UPDATE, typeCapture.getValue());
	}

}
