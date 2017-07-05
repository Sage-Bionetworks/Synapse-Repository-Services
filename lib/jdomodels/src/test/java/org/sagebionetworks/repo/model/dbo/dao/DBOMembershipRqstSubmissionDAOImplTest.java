package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMembershipRqstSubmissionDAOImplTest {
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private MembershipRqstSubmissionDAO membershipRqstSubmissionDAO;
	
	private Team team;
	private UserGroup individUser;
	private MembershipRqstSubmission mrs;
	
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
		mrs = new MembershipRqstSubmission();
		mrs.setCreatedOn(new Date());
		mrs.setExpiresOn(null); // NO EXPIRATION DATE
		mrs.setMessage("Please join the team.");
		mrs.setTeamId(team.getId());
		mrs.setUserId(individUser.getId());
	}
	
	@After
	public void tearDown() throws Exception {
		membershipRqstSubmissionDAO.delete(mrs.getId());
		teamDAO.delete(team.getId());
		userGroupDAO.delete(team.getId());
		userGroupDAO.delete(individUser.getId());
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		Date expiresOn = new Date();
		mrs.setExpiresOn(expiresOn);
		mrs = membershipRqstSubmissionDAO.create(mrs);
		assertNotNull(mrs.getId());
		
		// retrieve the mrs
		MembershipRqstSubmission clone = membershipRqstSubmissionDAO.get(mrs.getId());
		assertEquals(mrs, clone);
		
		// get-by-team query, returning only the *open* (unexpired) invitations
		// OK
		List<MembershipRequest> mrList = membershipRqstSubmissionDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, mrList.size());
		MembershipRequest mr = mrList.get(0);
		assertEquals(mrs.getMessage(), mr.getMessage());
		assertEquals(mrs.getExpiresOn(), mr.getExpiresOn());
		assertEquals(mrs.getTeamId(), mr.getTeamId());
		assertEquals(mrs.getUserId(), mr.getUserId());
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByTeamCount(teamId, expiresOn.getTime()-1000L));
		assertEquals(1, membershipRqstSubmissionDAO.getOpenRequestByTeamsCount(Arrays.asList(teamId.toString()), expiresOn.getTime()-1000L));
		// expired
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamCount(teamId, expiresOn.getTime()+1000L));
		// wrong team
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamInRange(-10L, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamCount(-10L, expiresOn.getTime()-1000L));
		// wrong page
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamCount(teamId, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		mrList = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, mrList.size());
		 mr = mrList.get(0);
		assertEquals(mrs.getMessage(), mr.getMessage());
		assertEquals(mrs.getExpiresOn(), mr.getExpiresOn());
		assertEquals(mrs.getTeamId(), mr.getTeamId());
		assertEquals(mrs.getUserId(), mr.getUserId());
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
		
		// expired
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()+1000L));
		// wrong team
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(-10L, pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(-10L, pgLong, expiresOn.getTime()-1000L));
		// wrong page
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// OK
		List<MembershipRqstSubmission> mrsList = membershipRqstSubmissionDAO.getOpenSubmissionsByRequesterInRange(pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, mrsList.size());
		assertEquals(mrs, mrsList.get(0));
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByRequesterCount(pgLong, expiresOn.getTime()-1000L));
		
		// expired
		assertEquals(0, membershipRqstSubmissionDAO.getOpenSubmissionsByRequesterInRange(pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByRequesterCount(pgLong, expiresOn.getTime()+1000L));
		// wrong page
		assertEquals(0, membershipRqstSubmissionDAO.getOpenSubmissionsByRequesterInRange(pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipRqstSubmissionDAO.getOpenSubmissionsByRequesterInRange(pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByRequesterCount(pgLong, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		mrsList = membershipRqstSubmissionDAO.getOpenSubmissionsByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, mrsList.size());
		assertEquals(mrs, mrsList.get(0));
		// expired
		assertEquals(0, membershipRqstSubmissionDAO.getOpenSubmissionsByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		// wrong team
		assertEquals(0, membershipRqstSubmissionDAO.getOpenSubmissionsByTeamAndRequesterInRange(-10L, pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		// wrong page
		assertEquals(0, membershipRqstSubmissionDAO.getOpenSubmissionsByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipRqstSubmissionDAO.getOpenSubmissionsByTeamAndRequesterInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
	}

	@Test
	public void testNoExpirationDate() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		// NO EXPIRATION DATE
		mrs = membershipRqstSubmissionDAO.create(mrs);
		assertNotNull(mrs.getId());

		// get-by-team query, returning only the *open* invitations
		// OK
		List<MembershipRequest> mrList = membershipRqstSubmissionDAO.getOpenByTeamInRange(teamId, (new Date()).getTime(), 1, 0);
		assertEquals(1, mrList.size());		
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByTeamCount(teamId, (new Date()).getTime()));

		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		mrList = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(1, mrList.size());
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, (new Date()).getTime()));
	}

	@Test
	public void testDeleteByTeamAndRequester() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		Date expiresOn = new Date();
		mrs.setExpiresOn(expiresOn);
		mrs = membershipRqstSubmissionDAO.create(mrs);
		assertNotNull(mrs.getId());
		
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
		
		membershipRqstSubmissionDAO.deleteByTeamAndRequester(teamId+1, pgLong);
		membershipRqstSubmissionDAO.deleteByTeamAndRequester(teamId, pgLong+1);
		// didn't delete our request
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
		membershipRqstSubmissionDAO.deleteByTeamAndRequester(teamId, pgLong);
		// now we did!
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
	}

}
