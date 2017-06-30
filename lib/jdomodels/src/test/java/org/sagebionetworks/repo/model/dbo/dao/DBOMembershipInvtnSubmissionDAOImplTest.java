package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMembershipInvtnSubmissionDAOImplTest {
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private MembershipInvtnSubmissionDAO membershipInvtnSubmissionDAO;
	
	private Team team;
	private UserGroup individUser;
	private MembershipInvtnSubmission mis;
	
	private static final String INVITER_USER_ID = "123";
	
	@Before
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
		team.setIcon("999");
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
		mis = new MembershipInvtnSubmission();
		mis.setCreatedOn(new Date());
		mis.setExpiresOn(null); // NO EXPIRATION DATE
		mis.setMessage("Please join the team.");
		mis.setTeamId(team.getId());
		mis.setInviteeId(individUser.getId());
		mis.setCreatedBy(INVITER_USER_ID);
	}
	
	@After
	public void tearDown() throws Exception {
		membershipInvtnSubmissionDAO.delete(mis.getId());
		teamDAO.delete(team.getId());
		userGroupDAO.delete(team.getId());
		userGroupDAO.delete(individUser.getId());
	}
	
	@Test 
	public void testNoExpirationDate() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		mis = membershipInvtnSubmissionDAO.create(mis);
		assertNotNull(mis.getId());
		
		// get-by-team query, returning only the *open* (unexpired) invitations
		// OK
		List<MembershipInvitation> miList = membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(1, miList.size());
		assertEquals(1, membershipInvtnSubmissionDAO.getOpenByUserCount(pgLong, (new Date()).getTime()));

		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		miList = membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(1, miList.size());
		assertEquals(1, membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(teamId, pgLong, (new Date()).getTime()));
		
		assertEquals(Collections.singletonList(INVITER_USER_ID), 
				membershipInvtnSubmissionDAO.getInvitersByTeamAndUser(teamId, pgLong, (new Date()).getTime()));
	}

	@Test
	public void testRoundTrip() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		Date expiresOn = new Date();
		mis.setExpiresOn(expiresOn);
		
		mis = membershipInvtnSubmissionDAO.create(mis);
		assertNotNull(mis.getId());
		
		// retrieve the mis
		MembershipInvtnSubmission clone = membershipInvtnSubmissionDAO.get(mis.getId());
		assertEquals(mis, clone);
		
		// get-by-team query, returning only the *open* (unexpired) invitations
		// OK
		List<MembershipInvitation> miList = membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, miList.size());
		MembershipInvitation mi = miList.get(0);
		assertEquals(mis.getMessage(), mi.getMessage());
		assertEquals(mis.getExpiresOn(), mi.getExpiresOn());
		assertEquals(""+pgLong, mi.getUserId());
		assertEquals(mis.getTeamId(), mi.getTeamId());
		assertEquals(1, membershipInvtnSubmissionDAO.getOpenByUserCount(pgLong, expiresOn.getTime()-1000L));
		
		// expired
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserCount(pgLong, expiresOn.getTime()+1000L));
		// wrong user
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserInRange(-10L, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserCount(-10L, expiresOn.getTime()-1000L));
		// wrong page
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()-1000, 2L, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserCount(pgLong, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		miList = membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, miList.size());
		mi = miList.get(0);
		assertEquals(mis.getMessage(), mi.getMessage());
		assertEquals(mis.getExpiresOn(), mi.getExpiresOn());
		assertEquals(""+pgLong, mi.getUserId());
		assertEquals(mis.getTeamId(), mi.getTeamId());
		assertEquals(1, membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));
		assertEquals(Collections.singletonList(INVITER_USER_ID), 
				membershipInvtnSubmissionDAO.getInvitersByTeamAndUser(teamId, pgLong, expiresOn.getTime()-1000L));

		// expired
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()+1000L));
		assertEquals(0, membershipInvtnSubmissionDAO.getInvitersByTeamAndUser(teamId, pgLong, expiresOn.getTime()+1000L).size());
		// wrong team
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(-10L, pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(-10L, pgLong, expiresOn.getTime()-1000L));
		assertEquals(0, membershipInvtnSubmissionDAO.getInvitersByTeamAndUser(-10L, pgLong, expiresOn.getTime()-1000L).size());
		// wrong user
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, -10L, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(teamId, -10L, expiresOn.getTime()-1000L));
		assertEquals(0, membershipInvtnSubmissionDAO.getInvitersByTeamAndUser(teamId, -10L, expiresOn.getTime()-1000L).size());
		// wrong page
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId, Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));
		assertEquals(0, membershipInvtnSubmissionDAO.getInvitersByTeamAndUser(teamId, pgLong, expiresOn.getTime()-1000L).size());
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// now test the query 'getOpenByTeamInRange'
		// OK
		List<MembershipInvtnSubmission> misList = membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamInRange(teamId, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, misList.size());
		assertEquals(mis, misList.get(0));

		assertEquals(1, membershipInvtnSubmissionDAO.getOpenByTeamCount(teamId, expiresOn.getTime()-1000L));

		// expired
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamInRange(teamId, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamCount(teamId, expiresOn.getTime()+1000L));
		// wrong team
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamInRange(-10L, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamCount(-10L, expiresOn.getTime()-1000L));
		// wrong page
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamInRange(teamId, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId, Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamInRange(teamId,  expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamCount(teamId, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));

		// OK
		misList = membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, misList.size());
		assertEquals(mis, misList.get(0));
		// expired
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		// wrong team
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamAndUserInRange(-10L, pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		// wrong user
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamAndUserInRange(teamId, -10L, expiresOn.getTime()-1000L, 1, 0).size());
		// wrong page
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId, Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenSubmissionsByTeamAndUserInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
	}
	
	@Test
	public void testDeleteByTeamAndUser() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		Date expiresOn = new Date();
		mis.setExpiresOn(expiresOn);
		
		mis = membershipInvtnSubmissionDAO.create(mis);
		assertNotNull(mis.getId());
		
		assertEquals(1, membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));

		membershipInvtnSubmissionDAO.deleteByTeamAndUser(teamId+1, pgLong);
		membershipInvtnSubmissionDAO.deleteByTeamAndUser(teamId, pgLong+1);
		// didn't delete our invitation
		assertEquals(1, membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));
		
		membershipInvtnSubmissionDAO.deleteByTeamAndUser(teamId, pgLong);
		// now we did!
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(teamId, pgLong, expiresOn.getTime()-1000L));
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

		mis = membershipInvtnSubmissionDAO.create(mis);

		List<MembershipInvitation> miList = membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(createdOn, miList.get(0).getCreatedOn());
		
		miList = membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(createdOn, miList.get(0).getCreatedOn());
	}
}
