package org.sagebionetworks.repo.web.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;
import static org.junit.Assert.*;

public class TeamServiceTest {
	
	private TeamServiceImpl teamService = new TeamServiceImpl();
	
	private TeamManager mockTeamManager;
	
	@Before
	public void before() throws Exception {
		mockTeamManager = mock(TeamManager.class);
		
		// Create UserGroups
		Collection<UserGroup> groups = new HashSet<UserGroup>();
		for (int i = 0; i < 10; i++) {
			UserGroup g = new UserGroup();
			g.setId("g" + i);
			g.setIsIndividual(false);
			g.setName("Group " + i);
			groups.add(g);
		}
		
		// Create UserProfiles
		List<UserProfile> list = new ArrayList<UserProfile>();
		for (int i = 0; i < 10; i++) {
			UserProfile p = new UserProfile();
			p.setOwnerId("p" + i);
			p.setDisplayName("User " + i);
			list.add(p);
		}
		// extra profile with duplicated name
		UserProfile p = new UserProfile();
		p.setOwnerId("p0_duplicate");
		p.setDisplayName("User 0");
		list.add(p);
		QueryResults<UserProfile> profiles = new QueryResults<UserProfile>(list, list.size());

		
		Map<Team, Collection<TeamMember>> universe = new HashMap<Team, Collection<TeamMember>>();
		when(mockTeamManager.getAllTeamsAndMembers()).thenReturn(universe);
		teamService.setTeamManager(mockTeamManager);
	}
	
	@Test
	public void testGetTeamsByFragment() throws Exception {
		List<Team> expected = new ArrayList<Team>();
		PaginatedResults<Team> pg = teamService.get("foo", 1, 0);
		assertEquals(expected.size(), pg.getTotalNumberOfResults());
		assertEquals(expected, pg.getResults());
	}
	

}
