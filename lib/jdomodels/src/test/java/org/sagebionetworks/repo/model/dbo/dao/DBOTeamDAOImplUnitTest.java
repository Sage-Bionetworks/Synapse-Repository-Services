package org.sagebionetworks.repo.model.dbo.dao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
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

/**
 * 
 * @author John
 *
 */
@ExtendWith(MockitoExtension.class)
public class DBOTeamDAOImplUnitTest {
	
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	@Mock
	private DBOBasicDao mockBasicDao;
	
	@InjectMocks
	private DBOTeamDAOImpl teamDao;
	
	private Team team;
	private DBOTeam teamDbo;
	
	@BeforeEach
	public void before(){
		
		team = new Team();
		team.setId("345");
		team.setEtag("etag");
		team.setCreatedBy("678");
		team.setCreatedOn(new Date());
		
		teamDbo = new DBOTeam();
		teamDbo.setId(Long.parseLong(team.getId()));
		teamDbo.setEtag(team.getEtag());
		TeamUtils.copyDtoToDbo(team, teamDbo);
	}

	@Test
	public void testCreateSendMessage(){
		when(mockBasicDao.createNew(any(DBOTeam.class))).thenReturn(teamDbo);
		
		// Call under test
		teamDao.create(team);
		
		verify(mockTransactionalMessenger).sendMessageAfterCommit(team.getId(), ObjectType.PRINCIPAL, ChangeType.CREATE);
	}
	
	@Test
	public void testUpdateSendMessage() throws Exception {
		when(mockNamedJdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(teamDbo);
		when(mockBasicDao.update(any(DatabaseObject.class))).thenReturn(true);
		
		// Call under test
		teamDao.update(team);
		
		verify(mockTransactionalMessenger).sendMessageAfterCommit(team.getId(), ObjectType.PRINCIPAL, ChangeType.UPDATE);
	}
	
	@Test
	public void testDeleteSendMessage() throws Exception{
		when(mockBasicDao.deleteObjectByPrimaryKey(any(), any())).thenReturn(true);
		
		// Call under test
		teamDao.delete(team.getId());
		
		verify(mockTransactionalMessenger).sendDeleteMessageAfterCommit(team.getId(), ObjectType.PRINCIPAL);
	}
}
