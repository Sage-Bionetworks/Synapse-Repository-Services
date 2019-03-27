package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 
 * @author John
 *
 */
public class DBOTeamDAOImplUnitTest {
	
	DBOTeamDAOImpl teamDao;
	@Mock
	TransactionalMessenger mockTransactionalMessenger;
	@Mock
	NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	@Mock
	DBOBasicDao mockBasicDao;
	Team team;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		teamDao = new DBOTeamDAOImpl();
		ReflectionTestUtils.setField(teamDao, "basicDao", mockBasicDao);
		ReflectionTestUtils.setField(teamDao, "transactionalMessenger", mockTransactionalMessenger);
		ReflectionTestUtils.setField(teamDao, "namedJdbcTemplate", mockNamedJdbcTemplate);
		team = new Team();
		team.setId("345");
		team.setEtag("etag");
		team.setCreatedBy("678");
		team.setCreatedOn(new Date());
		
		doAnswer(new Answer<DBOTeam>(){
			@Override
			public DBOTeam answer(InvocationOnMock invocation) throws Throwable {
				return (DBOTeam) invocation.getArguments()[0];
			}}).when(mockBasicDao).createNew(any(DBOTeam.class));
		DBOTeam dbo = new DBOTeam();
		dbo.setId(Long.parseLong(team.getId()));
		dbo.setEtag(team.getEtag());
		TeamUtils.copyDtoToDbo(team, dbo);
		when(mockNamedJdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(dbo);
		when(mockBasicDao.update(any(DatabaseObject.class))).thenReturn(true);
	}

	@Test
	public void testCreateSendMessage(){
		teamDao.create(team);
		ArgumentCaptor<String> idCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ObjectType> objectTypeCapture = ArgumentCaptor.forClass(ObjectType.class);
		ArgumentCaptor<String> etagCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ChangeType> typeCapture = ArgumentCaptor.forClass(ChangeType.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(idCapture.capture(), objectTypeCapture.capture(), etagCapture.capture(), typeCapture.capture());
		assertEquals(team.getId(), idCapture.getValue());
		assertEquals(ObjectType.PRINCIPAL, objectTypeCapture.getValue());
		assertNotNull(etagCapture.getValue());
		assertEquals(ChangeType.CREATE, typeCapture.getValue());
	}
	
	@Test
	public void testUpdateSendMessage() throws Exception{
		teamDao.update(team);
		ArgumentCaptor<String> idCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ObjectType> objectTypeCapture = ArgumentCaptor.forClass(ObjectType.class);
		ArgumentCaptor<String> etagCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ChangeType> typeCapture = ArgumentCaptor.forClass(ChangeType.class);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(idCapture.capture(), objectTypeCapture.capture(), etagCapture.capture(), typeCapture.capture());
		assertEquals(team.getId(), idCapture.getValue());
		assertEquals(ObjectType.PRINCIPAL, objectTypeCapture.getValue());
		assertNotNull(etagCapture.getValue());
		assertEquals(ChangeType.UPDATE, typeCapture.getValue());
	}
	
	@Test
	public void testDeleteSendMessage() throws Exception{
		teamDao.delete(team.getId());
		ArgumentCaptor<String> idCapture = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ObjectType> objectTypeCapture = ArgumentCaptor.forClass(ObjectType.class);
		ArgumentCaptor<ChangeType> typeCapture = ArgumentCaptor.forClass(ChangeType.class);
		verify(mockTransactionalMessenger).sendDeleteMessageAfterCommit(idCapture.capture(), objectTypeCapture.capture());
		assertEquals(team.getId(), idCapture.getValue());
		assertEquals(ObjectType.PRINCIPAL, objectTypeCapture.getValue());
	}
}
