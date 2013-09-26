package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMembershipInvtnSubmissionDAOImplTest {
	
	private long misToDelete = -1L;
	private long teamToDelete = -1L;
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private MembershipInvtnSubmissionDAO membershipInvtnSubmissionDAO;
	
	@After
	public void tearDown() throws Exception {
		if (membershipInvtnSubmissionDAO!=null && misToDelete!=-1L) {
			membershipInvtnSubmissionDAO.delete(""+misToDelete);
			misToDelete=-1L;
		}
		if (teamDAO!=null && teamToDelete!=-1L) {
			teamDAO.delete(""+teamToDelete);
			teamToDelete=-1L;
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		// create a team
		Team team = new Team();
		assertNotNull(userGroupDAO);
		UserGroup bug = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false);
		assertNotNull(bug);
		Long teamId = Long.parseLong(bug.getId());
		team.setId(""+teamId);
		team.setName("Super Team");
		team.setDescription("This is a Team designated for testing.");
		team.setIcon("999");
		team.setCreatedOn(new Date());
		team.setCreatedBy("101");
		team.setModifiedOn(new Date());
		team.setModifiedBy("102");
		teamDAO.create(team);
		teamToDelete = teamId;
		
		// create the submission
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		Date expiresOn = new Date();
		mis.setExpiresOn(expiresOn);
		mis.setMessage("Please join the team.");
		mis.setTeamId(""+teamId);
		
		// need another valid user group
		UserGroup individUser = userGroupDAO.findGroup(AuthorizationConstants.ANONYMOUS_USER_ID, true);
		mis.setInvitees(Arrays.asList(new String[]{individUser.getId()}));
		long pgLong = Long.parseLong(individUser.getId());
		
		mis = membershipInvtnSubmissionDAO.create(mis);
		String id = mis.getId();
		assertNotNull(id);
		misToDelete = Long.parseLong(id);
		
		// retrieve the mis
		MembershipInvtnSubmission clone = membershipInvtnSubmissionDAO.get(id);
		assertEquals(mis, clone);
		
		// get-by-team query, returning only the *open* (unexpired) invitations
		// OK
		List<MembershipInvitation> miList = membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()-1000L, 0, 1);
		assertEquals(1, miList.size());
		MembershipInvitation mi = miList.get(0);
		assertEquals(mis.getMessage(), mi.getMessage());
		assertEquals(mis.getExpiresOn(), mi.getExpiresOn());
		assertEquals(""+pgLong, mi.getUserId());
		assertEquals(mis.getTeamId(), mi.getTeam().getId());
		assertEquals(team.getName(), mi.getTeam().getName());
		assertEquals(team.getDescription(), mi.getTeam().getDescription());
		assertEquals(team.getIcon(), mi.getTeam().getIcon());
		
		// expired
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()+1000L, 0, 1).size());
		// wrong user
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserInRange(-10L, expiresOn.getTime()-1000L, 0, 1).size());
		// wrong page
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()-1000L, 1, 2).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByUserInRange(pgLong, expiresOn.getTime()-1000L, 0, 1).size());
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		miList = membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 0, 1);
		assertEquals(1, miList.size());
		 mi = miList.get(0);
		assertEquals(mis.getMessage(), mi.getMessage());
		assertEquals(mis.getExpiresOn(), mi.getExpiresOn());
		assertEquals(""+pgLong, mi.getUserId());
		assertEquals(mis.getTeamId(), mi.getTeam().getId());
		assertEquals(team.getName(), mi.getTeam().getName());
		assertEquals(team.getDescription(), mi.getTeam().getDescription());
		assertEquals(team.getIcon(), mi.getTeam().getIcon());

		// expired
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()+1000L, 0, 1).size());
		// wrong team
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(-10L, pgLong, expiresOn.getTime()-1000L, 0, 1).size());
		// wrong user
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, -10L, expiresOn.getTime()-1000L, 0, 1).size());
		// wrong page
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 2).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 0, 1).size());
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// delete the mrs
		membershipInvtnSubmissionDAO.delete(""+id);
		try {
			membershipInvtnSubmissionDAO.get(""+id);
			fail("Failed to delete "+id);
		} catch (NotFoundException e) {
			// OK
		}
		misToDelete=-1L; // no need to delete in 'tear down'
	}

}
