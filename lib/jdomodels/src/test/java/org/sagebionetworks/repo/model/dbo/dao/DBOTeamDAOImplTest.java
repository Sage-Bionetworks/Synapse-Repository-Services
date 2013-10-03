package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
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
public class DBOTeamDAOImplTest {
	
	private long teamToDelete = -1L;
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@After
	public void tearDown() throws Exception {
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
				
		assertEquals(1, teamDAO.getInRange(0, 1).size());
		assertEquals(0, teamDAO.getInRange(1, 2).size());
		assertEquals(1, teamDAO.getCount());
		
		assertEquals(0, teamDAO.getForMemberInRange(""+id, 0, 1).size());
		assertEquals(0, teamDAO.getForMemberInRange(""+id, 1, 3).size());
		assertEquals(0, teamDAO.getCountForMember(""+id));
		
		// need an arbitrary user to add to the group
		UserGroup pg = userGroupDAO.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		groupMembersDAO.addMembers(""+id, Arrays.asList(new String[]{pg.getId()}));
		assertEquals(1, teamDAO.getForMemberInRange(pg.getId(), 0, 1).size());
		assertEquals(0, teamDAO.getForMemberInRange(pg.getId(), 1, 3).size());
		assertEquals(1, teamDAO.getCountForMember(pg.getId()));
		groupMembersDAO.removeMembers(""+id,  Arrays.asList(new String[]{pg.getId()}));
		
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
