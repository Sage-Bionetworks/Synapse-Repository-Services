package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRequestDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class MembershipRequestDAOImplTest {
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private MembershipRequestDAO membershipRequestDAO;
	
	private Team team;
	private UserGroup individUser;
	private MembershipRequest mrs;
	
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
		mrs = new MembershipRequest();
		mrs.setCreatedOn(new Date());
		mrs.setExpiresOn(null); // NO EXPIRATION DATE
		mrs.setMessage("Please join the team.");
		mrs.setTeamId(team.getId());
		mrs.setUserId(individUser.getId());
	}
	
	@AfterEach
	public void tearDown() throws Exception {
		membershipRequestDAO.delete(mrs.getId());
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
		mrs = membershipRequestDAO.create(mrs);
		assertNotNull(mrs.getId());
		
		// retrieve the mrs
		MembershipRequest clone = membershipRequestDAO.get(mrs.getId());
		assertEquals(mrs, clone);
		
		// get-by-team query, returning only the *open* (unexpired) invitations
		// OK
		List<MembershipRequest> mrList = membershipRequestDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, mrList.size());
		MembershipRequest mr = mrList.get(0);
		assertEquals(mrs.getMessage(), mr.getMessage());
		assertEquals(mrs.getExpiresOn(), mr.getExpiresOn());
		assertEquals(mrs.getTeamId(), mr.getTeamId());
		assertEquals(mrs.getUserId(), mr.getUserId());
		assertEquals(1, membershipRequestDAO.getOpenByTeamCount(teamId, expiresOn.getTime()-1000L));
		assertEquals(1, membershipRequestDAO.getOpenByTeamsCount(Arrays.asList(teamId.toString()), expiresOn.getTime()-1000L));
		// expired
		assertEquals(0, membershipRequestDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipRequestDAO.getOpenByTeamCount(teamId, expiresOn.getTime()+1000L));
		// wrong team
		assertEquals(0, membershipRequestDAO.getOpenByTeamInRange(-10L, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRequestDAO.getOpenByTeamCount(-10L, expiresOn.getTime()-1000L));
		// wrong page
		assertEquals(0, membershipRequestDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipRequestDAO.getOpenByTeamInRange(teamId, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRequestDAO.getOpenByTeamCount(teamId, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		mrList = membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, mrList.size());
		 mr = mrList.get(0);
		assertEquals(mrs.getMessage(), mr.getMessage());
		assertEquals(mrs.getExpiresOn(), mr.getExpiresOn());
		assertEquals(mrs.getTeamId(), mr.getTeamId());
		assertEquals(mrs.getUserId(), mr.getUserId());
		assertEquals(1, membershipRequestDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
		
		// expired
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()+1000L));
		// wrong team
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterInRange(-10L, pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterCount(-10L, pgLong, expiresOn.getTime()-1000L));
		// wrong page
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// OK
		List<MembershipRequest> mrsList = membershipRequestDAO.getOpenByRequesterInRange(pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, mrsList.size());
		assertEquals(mrs, mrsList.get(0));
		assertEquals(1, membershipRequestDAO.getOpenByRequesterCount(pgLong, expiresOn.getTime()-1000L));
		
		// expired
		assertEquals(0, membershipRequestDAO.getOpenByRequesterInRange(pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipRequestDAO.getOpenByRequesterCount(pgLong, expiresOn.getTime()+1000L));
		// wrong page
		assertEquals(0, membershipRequestDAO.getOpenByRequesterInRange(pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipRequestDAO.getOpenByRequesterInRange(pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRequestDAO.getOpenByRequesterCount(pgLong, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		mrsList = membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, mrsList.size());
		assertEquals(mrs, mrsList.get(0));
		// expired
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		// wrong team
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterInRange(-10L, pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		// wrong page
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
	}

	@Test
	public void testNoExpirationDate() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		// NO EXPIRATION DATE
		mrs = membershipRequestDAO.create(mrs);
		assertNotNull(mrs.getId());

		// get-by-team query, returning only the *open* invitations
		// OK
		List<MembershipRequest> mrList = membershipRequestDAO.getOpenByTeamInRange(teamId, (new Date()).getTime(), 1, 0);
		assertEquals(1, mrList.size());		
		assertEquals(1, membershipRequestDAO.getOpenByTeamCount(teamId, (new Date()).getTime()));

		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		mrList = membershipRequestDAO.getOpenByTeamAndRequesterInRange(teamId, pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(1, mrList.size());
		assertNotNull(mrList.get(0).getCreatedOn());
		assertEquals(1, membershipRequestDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, (new Date()).getTime()));
	}

	@Test
	public void testDeleteByTeamAndRequester() throws Exception {
		Long teamId = Long.parseLong(team.getId());
		Long pgLong = Long.parseLong(individUser.getId());
		
		Date expiresOn = new Date();
		mrs.setExpiresOn(expiresOn);
		mrs = membershipRequestDAO.create(mrs);
		assertNotNull(mrs.getId());
		
		assertEquals(1, membershipRequestDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
		
		membershipRequestDAO.deleteByTeamAndRequester(teamId+1, pgLong);
		membershipRequestDAO.deleteByTeamAndRequester(teamId, pgLong+1);
		// didn't delete our request
		assertEquals(1, membershipRequestDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
		membershipRequestDAO.deleteByTeamAndRequester(teamId, pgLong);
		// now we did!
		assertEquals(0, membershipRequestDAO.getOpenByTeamAndRequesterCount(teamId, pgLong, expiresOn.getTime()-1000L));
	}

}
