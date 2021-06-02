package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvitationDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipInvitation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class MembershipInvitationDAOImplTest {
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private MembershipInvitationDAO membershipInvitationDAO;

	@Autowired DBOBasicDao basicDAO;
	
	private Team team;
	private UserGroup individUser;
	private MembershipInvitation mis;
	
	private static final String INVITER_USER_ID = "123";
	
	@BeforeEach
	public void before() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		group.setId(userGroupDAO.create(group).toString());
		Long groupId = Long.parseLong(group.getId());
		
		// create a team
		team = new Team();
		assertNotNull(userGroupDAO);
		team.setId(groupId.toString());
		team.setName("Super Team");
		team.setDescription("This is a Team designated for testing.");
		team.setCreatedOn(new Date());
		team.setCreatedBy("101");
		team.setModifiedOn(new Date());
		team.setModifiedBy("102");
		team = teamDAO.create(team);
		
		// Create another user
		individUser = new UserGroup();
		individUser.setIsIndividual(true);
		individUser.setId(userGroupDAO.create(individUser).toString());
		
		// Initialize the submission but let the tests create it
		mis = new MembershipInvitation();
		mis.setCreatedOn(new Date());
		mis.setExpiresOn(null); // NO EXPIRATION DATE
		mis.setMessage("Please join the team.");
		mis.setTeamId(team.getId());
		mis.setInviteeId(individUser.getId());
		mis.setCreatedBy(INVITER_USER_ID);
	}
	
	@AfterEach
	public void tearDown() throws Exception {
		membershipInvitationDAO.delete(mis.getId());
		teamDAO.delete(team.getId());
		userGroupDAO.delete(team.getId());
		userGroupDAO.delete(individUser.getId());
	}
	
	@Test 
	public void testNoExpirationDate() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		mis = membershipInvitationDAO.create(mis);
		assertNotNull(mis.getId());
		
		// get-by-team query, returning only the *open* (unexpired) invitations
		// OK
		List<MembershipInvitation> miList = membershipInvitationDAO.getOpenByUserInRange(pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(1, miList.size());
		assertEquals(1, membershipInvitationDAO.getOpenByUserCount(pgLong, (new Date()).getTime()));

		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		miList = membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(1, miList.size());
		assertEquals(1, membershipInvitationDAO.getOpenByTeamAndUserCount(teamId, pgLong, (new Date()).getTime()));
		
		assertEquals(Collections.singletonList(INVITER_USER_ID), 
				membershipInvitationDAO.getInvitersByTeamAndUser(teamId, pgLong, (new Date()).getTime()));
	}

	@Test
	public void testRoundTrip() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		Date expiresOn = new Date();
		mis.setExpiresOn(expiresOn);
		
		mis = membershipInvitationDAO.create(mis);
		assertNotNull(mis.getId());
		
		// retrieve the mis
		MembershipInvitation clone = membershipInvitationDAO.get(mis.getId());
		assertEquals(mis, clone);
		
		// get-by-team query, returning only the *open* (unexpired) invitations
		// OK
		List<MembershipInvitation> miList = membershipInvitationDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, miList.size());
		MembershipInvitation mis = miList.get(0);
		assertEquals(mis.getMessage(), mis.getMessage());
		assertEquals(mis.getExpiresOn(), mis.getExpiresOn());
		assertEquals(""+pgLong, mis.getInviteeId());
		assertEquals(mis.getTeamId(), mis.getTeamId());
		assertEquals(1, membershipInvitationDAO.getOpenByUserCount(pgLong, expiresOn.getTime()-1000L));
		
		// expired
		assertEquals(0, membershipInvitationDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByUserCount(pgLong, expiresOn.getTime()+1000L));
		// wrong user
		assertEquals(0, membershipInvitationDAO.getOpenByUserInRange(-10L, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByUserCount(-10L, expiresOn.getTime()-1000L));
		// wrong page
		assertEquals(0, membershipInvitationDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()-1000, 2L, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvitationDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByUserCount(pgLong, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		miList = membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, miList.size());
		mis = miList.get(0);
		assertEquals(mis.getMessage(), mis.getMessage());
		assertEquals(mis.getExpiresOn(), mis.getExpiresOn());
		assertEquals(""+pgLong, mis.getInviteeId());
		assertEquals(mis.getTeamId(), mis.getTeamId());
		assertEquals(1, membershipInvitationDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));
		assertEquals(Collections.singletonList(INVITER_USER_ID), 
				membershipInvitationDAO.getInvitersByTeamAndUser(teamId, pgLong, expiresOn.getTime()-1000L));

		// expired
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()+1000L));
		assertEquals(0, membershipInvitationDAO.getInvitersByTeamAndUser(teamId, pgLong, expiresOn.getTime()+1000L).size());
		// wrong team
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(-10L, pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserCount(-10L, pgLong, expiresOn.getTime()-1000L));
		assertEquals(0, membershipInvitationDAO.getInvitersByTeamAndUser(-10L, pgLong, expiresOn.getTime()-1000L).size());
		// wrong user
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, -10L, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserCount(teamId, -10L, expiresOn.getTime()-1000L));
		assertEquals(0, membershipInvitationDAO.getInvitersByTeamAndUser(teamId, -10L, expiresOn.getTime()-1000L).size());
		// wrong page
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId, Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));
		assertEquals(0, membershipInvitationDAO.getInvitersByTeamAndUser(teamId, pgLong, expiresOn.getTime()-1000L).size());
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// now test the query 'getOpenByTeamInRange'
		// OK
		List<MembershipInvitation> misList = membershipInvitationDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, misList.size());
		assertEquals(mis, misList.get(0));

		assertEquals(1, membershipInvitationDAO.getOpenByTeamCount(teamId, expiresOn.getTime()-1000L));

		// expired
		assertEquals(0, membershipInvitationDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByTeamCount(teamId, expiresOn.getTime()+1000L));
		// wrong team
		assertEquals(0, membershipInvitationDAO.getOpenByTeamInRange(-10L, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByTeamCount(-10L, expiresOn.getTime()-1000L));
		// wrong page
		assertEquals(0, membershipInvitationDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId, Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvitationDAO.getOpenByTeamInRange(teamId,  expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvitationDAO.getOpenByTeamCount(teamId, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));

		// OK
		misList = membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, misList.size());
		assertEquals(mis, misList.get(0));
		// expired
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		// wrong team
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(-10L, pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		// wrong user
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, -10L, expiresOn.getTime()-1000L, 1, 0).size());
		// wrong page
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId, Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
	}
	
	@Test
	public void testDeleteByTeamAndUser() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		Date expiresOn = new Date();
		mis.setExpiresOn(expiresOn);
		
		mis = membershipInvitationDAO.create(mis);
		assertNotNull(mis.getId());
		
		assertEquals(1, membershipInvitationDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));

		membershipInvitationDAO.deleteByTeamAndUser(teamId+1, pgLong);
		membershipInvitationDAO.deleteByTeamAndUser(teamId, pgLong+1);
		// didn't delete our invitation
		assertEquals(1, membershipInvitationDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));
		
		membershipInvitationDAO.deleteByTeamAndUser(teamId, pgLong);
		// now we did!
		assertEquals(0, membershipInvitationDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));
	}
	
	/*
	 * PLFM-4479
	 */
	@Test
	public void testCreatedOn() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());

		Date createdOn = new Date();
		mis.setCreatedOn(createdOn);

		mis = membershipInvitationDAO.create(mis);

		List<MembershipInvitation> miList = membershipInvitationDAO.getOpenByUserInRange(pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(createdOn, miList.get(0).getCreatedOn());
		
		miList = membershipInvitationDAO.getOpenByTeamAndUserInRange(teamId, pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(createdOn, miList.get(0).getCreatedOn());
	}

	@Test
	public void testUpdateInviteeId() {
		// Create a mis with null invitee id
		mis.setInviteeId(null);
		MembershipInvitation dto = membershipInvitationDAO.create(mis);
		String misId = dto.getId();

		// Get the invitation's etag
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOConstants.PARAM_EVALUATION_ID, misId);
		DBOMembershipInvitation dbo = basicDAO.getObjectByPrimaryKey(DBOMembershipInvitation.class, param);
		String oldEtag = dbo.getEtag();

		// Update the inviteeId and get the updated invitation
		String inviteeId = individUser.getId();
		membershipInvitationDAO.updateInviteeId(misId, Long.parseLong(inviteeId));
		dbo = basicDAO.getObjectByPrimaryKey(DBOMembershipInvitation.class, param);

		// inviteeId should be updated
		assertEquals(inviteeId, dbo.getInviteeId().toString());
		// etag should be different
		assertNotEquals(oldEtag, dbo.getEtag());
	}
}
