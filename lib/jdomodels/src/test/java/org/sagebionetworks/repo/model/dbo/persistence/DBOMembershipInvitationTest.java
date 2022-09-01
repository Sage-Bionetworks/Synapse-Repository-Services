package org.sagebionetworks.repo.model.dbo.persistence;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TeamState;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMembershipInvitationTest {
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
		
	private List<Long> toDelete = null;
	private List<Long> teamToDelete = null;
	
	@AfterEach
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOMembershipInvitation.class, params);
			}
		}
		if(dboBasicDao != null && teamToDelete != null){
			for(Long id: teamToDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOTeam.class, params);
			}
		}
	}
	
	@BeforeEach
	public void before(){
		toDelete = new LinkedList<Long>();
		teamToDelete = new LinkedList<Long>();
	}
	
	public static DBOMembershipInvitation newMembershipInvitation(
			IdGenerator idGenerator, 
			DBOBasicDao dboBasicDao) {
		DBOMembershipInvitation invitation = new DBOMembershipInvitation();
		invitation.setId(idGenerator.generateNewId(IdType.MEMBERSHIP_INVITATION_ID));
		invitation.setEtag(DBOMembershipInvitation.defaultEtag);
		invitation.setCreatedOn(System.currentTimeMillis());
		invitation.setExpiresOn(System.currentTimeMillis());
		DBOTeam team = DBOTeamTest.newTeam();
		team = dboBasicDao.createNew(team);
		team.setState(TeamState.OPEN.name());
		invitation.setTeamId(team.getId());
		invitation.setProperties((new String("abcdefg")).getBytes());
		Long invitee = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		invitation.setInviteeId(invitee);
		return invitation;
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException {
		DBOMembershipInvitation invitation = newMembershipInvitation(idGenerator, dboBasicDao);
		// Make sure we can create it
		DBOMembershipInvitation clone = dboBasicDao.createNew(invitation);
		toDelete.add(invitation.getId());
		teamToDelete.add(invitation.getTeamId());
		assertNotNull(clone);
		assertEquals(invitation, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", invitation.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOMembershipInvitation.class, params).get();
		assertNotNull(clone);
		assertEquals(invitation, clone);
		
		// Make sure we can update it.
		clone.setProperties(new byte[] { (byte)1 });
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		DBOMembershipInvitation clone2 = dboBasicDao.getObjectByPrimaryKey(DBOMembershipInvitation.class, params).get();
		assertEquals(clone, clone2);
	}
}