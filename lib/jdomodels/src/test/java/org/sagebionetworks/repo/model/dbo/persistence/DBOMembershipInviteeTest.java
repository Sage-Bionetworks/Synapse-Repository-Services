package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMembershipInviteeTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<Long> toDelete = null;
	private List<Long> invitationToDelete = null;
	private List<Long> teamToDelete = null;
	
	@After
	public void after() throws DatastoreException {
		if (dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("invitationId", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOMembershipInvitee.class, params);
			}
		}
		if (dboBasicDao != null && invitationToDelete != null){
			for(Long id: invitationToDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOMembershipInvtnSubmission.class, params);
			}
		}
		if (dboBasicDao != null && teamToDelete != null){
			for(Long id: teamToDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOTeam.class, params);
			}
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
		invitationToDelete = new LinkedList<Long>();
		teamToDelete = new LinkedList<Long>();
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException{

		DBOMembershipInvtnSubmission invitation = 
				DBOMembershipInvtnSubmissionTest.newMembershipInvtnSubmission(
						idGenerator, userGroupDAO, dboBasicDao);
		teamToDelete.add(invitation.getTeamId());
		invitation = dboBasicDao.createNew(invitation);
		assertNotNull(invitation);
		assertNotNull(invitation.getId());
		DBOMembershipInvitee record = new DBOMembershipInvitee();
		record.setInvitationId(invitation.getId());
		Long userId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		record.setInviteeId(userId);
		// Make sure we can create it
		DBOMembershipInvitee clone = dboBasicDao.createNew(record);
		toDelete.add(record.getInvitationId());
		invitationToDelete.add(record.getInvitationId());
		
		assertNotNull(clone);
		assertEquals(record, clone);
		
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("invitationId", record.getInvitationId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOMembershipInvitee.class, params);
		assertNotNull(clone);
		assertEquals(record, clone);
	}

}
