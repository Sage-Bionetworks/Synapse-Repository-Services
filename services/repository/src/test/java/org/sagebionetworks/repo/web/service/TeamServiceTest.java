package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroupHeader;

public class TeamServiceTest {
	
	private TeamServiceImpl teamService = new TeamServiceImpl();
	
	private TeamManager mockTeamManager;
	
	private Team team = null;
	private TeamMember member = null;
	
	@Before
	public void before() throws Exception {
		mockTeamManager = mock(TeamManager.class);
			
		team = new Team();
		team.setId("101");
		team.setName("foo bar");
		member = new TeamMember();
		UserGroupHeader ugh = new UserGroupHeader();
		ugh.setEmail("rogue.user@gmail.com");
		ugh.setDisplayName("John Smith");
		member.setMember(ugh);

		Map<Team, Collection<TeamMember>> universe = new HashMap<Team, Collection<TeamMember>>();
		universe.put(team, Arrays.asList(new TeamMember[]{member}));
		when(mockTeamManager.getAllTeamsAndMembers()).thenReturn(universe);
		teamService.setTeamManager(mockTeamManager);
	}
	
	@Test
	public void testGetTeamsByFragment() throws Exception {
		List<Team> expected = new ArrayList<Team>(); expected.add(team);
		PaginatedResults<Team> pr = teamService.get("foo", 1, 0);
		assertEquals(expected.size(), pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		pr = teamService.get("ba", 1, 0);
		assertEquals(expected.size(), pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		
		// no match
		pr = teamService.get("bas", 1, 0);
		assertEquals(0, pr.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetTeamMembersByFragment() throws Exception {
		List<TeamMember> expected = new ArrayList<TeamMember>(); expected.add(member);
		// test last name match
		PaginatedResults<TeamMember> pr = teamService.getMembers("101", "Smith", 1, 0);
		assertEquals(expected.size(), pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		// test first name match, different case
		pr = teamService.getMembers("101", "john", 1, 0);
		assertEquals(expected.size(), pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		// test first email match, different case
		pr = teamService.getMembers("101", "rogue.USER", 1, 0);
		assertEquals(expected.size(), pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		// no match
		pr = teamService.getMembers("101", "bas", 1, 0);
		assertEquals(0, pr.getTotalNumberOfResults());
		
		// in the process of creating the cache the email addresses are obfuscated
		assertEquals("rog...r@gmail.com", member.getMember().getEmail());
	}
	

}
