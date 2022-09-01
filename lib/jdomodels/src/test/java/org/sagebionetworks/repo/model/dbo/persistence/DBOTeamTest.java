package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamState;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.TeamUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOTeamTest {
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	List<Long> toDelete = null;
	
	@AfterEach
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOTeam.class, params);
			}
		}
	}
	
	@BeforeEach
	public void before(){
		toDelete = new LinkedList<Long>();
	}
	
	public static DBOTeam newTeam() {
		Long id = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		DBOTeam team = new DBOTeam();
		team.setId(id);
		team.setEtag("1");
		team.setProperties((new String("12345")).getBytes());
		return team;
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException{
		// Make sure we can create it
		DBOTeam team = newTeam();
		toDelete.add(team.getId());
		DBOTeam clone = dboBasicDao.createNew(team);		
		assertNotNull(clone);
		assertEquals(team, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", team.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOTeam.class, params).get();
		assertNotNull(clone);
		assertEquals(team, clone);
		
		// Make sure we can update it.
		clone.setProperties(new byte[] { (byte)1 });
		clone.setEtag("2");
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		DBOTeam clone2 = dboBasicDao.getObjectByPrimaryKey(DBOTeam.class, params).get();
		assertEquals(clone, clone2);
	}

	@TemporaryCode(author = "peter.harvey@sagebase.org", comment = "One time migration of state.  Can be removed after all teams have a state value.")
	@Test
	public void testCreateDatabaseObjectFromBackupWithPublicTeam() {
		Team dto = new Team();
		dto.setId("123");
		dto.setEtag(UUID.randomUUID().toString());
		dto.setCanPublicJoin(true);
		dto.setCanRequestMembership(false);
		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);

		// Call under test
		dbo = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);

		assertEquals(TeamState.PUBLIC.name(), dbo.getState());
	}

	@TemporaryCode(author = "peter.harvey@sagebase.org", comment = "One time migration of state.  Can be removed after all teams have a state value.")
	@Test
	public void testCreateDatabaseObjectFromBackupWithOpenTeam() {
		Team dto = new Team();
		dto.setId("123");
		dto.setEtag(UUID.randomUUID().toString());
		dto.setCanPublicJoin(false);
		dto.setCanRequestMembership(true);
		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);

		// Call under test
		dbo = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);

		assertEquals(TeamState.OPEN.name(), dbo.getState());
	}

	@TemporaryCode(author = "peter.harvey@sagebase.org", comment = "One time migration of state.  Can be removed after all teams have a state value.")
	@Test
	public void testCreateDatabaseObjectFromBackupWithClosedTeam() {
		Team dto = new Team();
		dto.setId("123");
		dto.setEtag(UUID.randomUUID().toString());
		dto.setCanPublicJoin(false);
		dto.setCanRequestMembership(false);
		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);

		// Call under test
		dbo = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);

		assertEquals(TeamState.CLOSED.name(), dbo.getState());
	}
}
