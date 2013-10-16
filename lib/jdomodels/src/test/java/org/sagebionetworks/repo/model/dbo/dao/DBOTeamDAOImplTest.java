package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamHeader;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOTeamDAOImplTest {
	
	private long teamToDelete = -1L;
	private long upToDelete = -1L;
	private String[] teamMemberPairToDelete = null;
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@After
	public void tearDown() throws Exception {
		if (teamDAO!=null && teamToDelete!=-1L) {
			teamDAO.delete(""+teamToDelete);
			teamToDelete=-1L;
		}
		if (userProfileDAO!=null && upToDelete!=-1L) {
			userProfileDAO.delete(""+upToDelete);
			upToDelete = -1L;
		}
		if (groupMembersDAO!=null && teamMemberPairToDelete!=null) {
			groupMembersDAO.removeMembers(teamMemberPairToDelete[0],  Arrays.asList(new String[]{teamMemberPairToDelete[1]}));

		}
	}
	
	private static TeamHeader createTeamHeaderFromTeam(Team team) {
		TeamHeader th = new TeamHeader();
		th.setId(team.getId());
		th.setName(team.getName());
		return th;
	}
	
	private static UserProfile createUserProfileForGroup(UserGroup ug) {
		UserProfile up = new UserProfile();
		up.setEmail(ug.getName());
		up.setOwnerId(ug.getId());
		return up;
	}
	
	private static UserGroupHeader createUserGroupHeaderFromUserProfile(UserProfile up) {
		UserGroupHeader ugh = new UserGroupHeader();
		ugh.setOwnerId(up.getOwnerId());
		ugh.setEmail(up.getEmail());
		ugh.setIsIndividual(true);
		return ugh;
	}


	@Test
	public void testRoundTrip() throws Exception {
		// create a team
		Team team = new Team();
		assertNotNull(userGroupDAO);
		UserGroup bug = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false);
		assertNotNull(bug);
		Long id = Long.parseLong(bug.getId());
		team.setId(""+id);
		team.setName("Super Team");
		team.setDescription("This is a Team designated for testing.");
		team.setIcon("999");
		team.setCreatedOn(new Date());
		team.setCreatedBy("101");
		team.setModifiedOn(new Date());
		team.setModifiedBy("102");
		Team createdTeam = teamDAO.create(team);
		teamToDelete = id;
		assertNotNull(createdTeam.getEtag());
		createdTeam.setEtag(null); // to allow comparison with 'team'
		assertEquals(team, createdTeam);
		// retrieve the team
		Team clone = teamDAO.get(""+id);
		team.setEtag(clone.getEtag()); // for comparison
		assertEquals(team, clone);
		// update the team
		clone.setDescription("this is the modified description");
		clone.setIcon("111");
		Team updated = teamDAO.update(clone);
		clone.setEtag(updated.getEtag()); // for comparison
		assertEquals(clone, updated);
		
		Team retrieved = teamDAO.get(updated.getId());
		assertEquals(updated, retrieved);
				
		assertEquals(1, teamDAO.getInRange(1, 0).size());
		assertEquals(0, teamDAO.getInRange(2, 1).size());
		assertEquals(1, teamDAO.getCount());
		
		assertEquals(0, teamDAO.getForMemberInRange(""+id, 1, 0).size());
		assertEquals(0, teamDAO.getForMemberInRange(""+id, 3, 1).size());
		assertEquals(0, teamDAO.getCountForMember(""+id));
		
		assertEquals(new HashMap<TeamHeader,List<UserGroupHeader>>(), teamDAO.getAllTeamsAndMembers());

		// need an arbitrary user to add to the group
		UserGroup pg = userGroupDAO.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		groupMembersDAO.addMembers(""+id, Arrays.asList(new String[]{pg.getId()}));
		teamMemberPairToDelete = new String[] {""+id, pg.getId()};
		assertEquals(1, teamDAO.getForMemberInRange(pg.getId(), 1, 0).size());
		assertEquals(0, teamDAO.getForMemberInRange(pg.getId(), 3, 1).size());
		assertEquals(1, teamDAO.getCountForMember(pg.getId()));
		
		UserProfile up = createUserProfileForGroup(pg);
		userProfileDAO.create(up);
		upToDelete = Long.parseLong(up.getOwnerId());
		Map<Team,Collection<TeamMember>> expectedAllTeamsAndMembers = new HashMap<Team,Collection<TeamMember>>();
		UserGroupHeader ugh = createUserGroupHeaderFromUserProfile(up);
		TeamMember tm = new TeamMember();
		tm.setIsAdmin(false);
		tm.setMember(ugh);
		tm.setTeamId(""+id);
		List<TeamMember> tmList = new ArrayList<TeamMember>();
		tmList.add(tm);
		expectedAllTeamsAndMembers.put(updated,  tmList);
		
		// we have to check 'equals' on the pieces because a global 'assertEquals' fails
		Map<Team,Collection<TeamMember>> actualAllTeamsAndMembers = teamDAO.getAllTeamsAndMembers();
		assertEquals(expectedAllTeamsAndMembers.size(), actualAllTeamsAndMembers.size());
		for (Team t : expectedAllTeamsAndMembers.keySet()) {
			Collection<TeamMember> expectedTeamMembers = expectedAllTeamsAndMembers.get(t);
			Collection<TeamMember> actualTeamMembers = actualAllTeamsAndMembers.get(t);
			assertNotNull("Missing key "+t, actualTeamMembers);
			assertEquals(expectedTeamMembers.size(), actualTeamMembers.size());
			for (TeamMember m : expectedTeamMembers) {
				assertTrue(actualTeamMembers.contains(m));
			}
		}
		
		groupMembersDAO.removeMembers(""+id,  Arrays.asList(new String[]{pg.getId()}));
		teamMemberPairToDelete = null; // no longer need to schedule for deletion
		
		// delete the team
		teamDAO.delete(""+id);
		try {
			teamDAO.get(""+id);
			fail("Failed to delete "+id);
		} catch (NotFoundException e) {
			// OK
		}
		teamToDelete=-1L; // no need to delete in 'tear down'
	}

}
