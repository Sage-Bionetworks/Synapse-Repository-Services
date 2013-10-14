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
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
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
public class DBOMembershipRqstSubmissionDAOImplTest {
	
	private long mrsToDelete = -1L;
	private long teamToDelete = -1L;
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private MembershipRqstSubmissionDAO membershipRqstSubmissionDAO;
	
	@After
	public void tearDown() throws Exception {
		if (membershipRqstSubmissionDAO!=null && mrsToDelete!=-1L) {
			membershipRqstSubmissionDAO.delete(""+mrsToDelete);
			mrsToDelete=-1L;
		}
		if (teamDAO!=null && teamToDelete!=-1L) {
			teamDAO.delete(""+teamToDelete);
			teamToDelete=-1L;
		}
	}
	
	private Long createTeam() throws Exception {
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
		return teamId;
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		Long teamId = createTeam();
		// create the submission
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		Date expiresOn = new Date();
		mrs.setCreatedOn(new Date());
		mrs.setExpiresOn(expiresOn);
		mrs.setMessage("Please let me join the team.");
		mrs.setTeamId(""+teamId);
		
		// need another valid user group
		UserGroup individUser = userGroupDAO.findGroup(AuthorizationConstants.ANONYMOUS_USER_ID, true);
		mrs.setUserId(individUser.getId());
		
		mrs = membershipRqstSubmissionDAO.create(mrs);
		String id = mrs.getId();
		assertNotNull(id);
		mrsToDelete = Long.parseLong(id);
		
		// retrieve the mrs
		MembershipRqstSubmission clone = membershipRqstSubmissionDAO.get(id);
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
		long pgLong = Long.parseLong(individUser.getId());
		mrList = membershipRqstSubmissionDAO.getOpenByTeamAndRequestorInRange(teamId, pgLong, expiresOn.getTime()-1000L, 1, 0);
		assertEquals(1, mrList.size());
		 mr = mrList.get(0);
		assertEquals(mrs.getMessage(), mr.getMessage());
		assertEquals(mrs.getExpiresOn(), mr.getExpiresOn());
		assertEquals(mrs.getTeamId(), mr.getTeamId());
		assertEquals(mrs.getUserId(), mr.getUserId());
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByTeamAndRequestorCount(teamId, pgLong, expiresOn.getTime()-1000L));
		
		// expired
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequestorInRange(teamId, pgLong, expiresOn.getTime()+1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequestorCount(teamId, pgLong, expiresOn.getTime()+1000L));
		// wrong team
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequestorInRange(-10L, pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequestorCount(-10L, pgLong, expiresOn.getTime()-1000L));
		// wrong page
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequestorInRange(teamId, pgLong, expiresOn.getTime()-1000L, 2, 1).size());
		// already in team
		groupMembersDAO.addMembers(""+teamId,     Arrays.asList(new String[]{individUser.getId()}));
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequestorInRange(teamId,  pgLong, expiresOn.getTime()-1000L, 1, 0).size());
		assertEquals(0, membershipRqstSubmissionDAO.getOpenByTeamAndRequestorCount(teamId, pgLong, expiresOn.getTime()-1000L));
		groupMembersDAO.removeMembers(""+teamId,  Arrays.asList(new String[]{individUser.getId()}));
		
		// delete the mrs
		membershipRqstSubmissionDAO.delete(""+id);
		try {
			membershipRqstSubmissionDAO.get(""+id);
			fail("Failed to delete "+id);
		} catch (NotFoundException e) {
			// OK
		}
		mrsToDelete=-1L; // no need to delete in 'tear down'
	}

	@Test
	public void testNoExpirationDate() throws Exception {
		Long teamId = createTeam();
		// create the submission
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setCreatedOn(new Date());
		mrs.setExpiresOn(null); // NO EXPIRATION DATE
		mrs.setMessage("Please let me join the team.");
		mrs.setTeamId(""+teamId);
		
		// need another valid user group
		UserGroup individUser = userGroupDAO.findGroup(AuthorizationConstants.ANONYMOUS_USER_ID, true);
		mrs.setUserId(individUser.getId());
		
		mrs = membershipRqstSubmissionDAO.create(mrs);
		String id = mrs.getId();
		assertNotNull(id);
		mrsToDelete = Long.parseLong(id);

		// get-by-team query, returning only the *open* invitations
		// OK
		List<MembershipRequest> mrList = membershipRqstSubmissionDAO.getOpenByTeamInRange(teamId, (new Date()).getTime(), 1, 0);
		assertEquals(1, mrList.size());		
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByTeamCount(teamId, (new Date()).getTime()));

		// get-by-team-and-user query, returning only the *open* (unexpired) invitations
		// OK
		long pgLong = Long.parseLong(individUser.getId());
		mrList = membershipRqstSubmissionDAO.getOpenByTeamAndRequestorInRange(teamId, pgLong, (new Date()).getTime(), 1, 0);
		assertEquals(1, mrList.size());
		assertEquals(1, membershipRqstSubmissionDAO.getOpenByTeamAndRequestorCount(teamId, pgLong, (new Date()).getTime()));
	}


}
