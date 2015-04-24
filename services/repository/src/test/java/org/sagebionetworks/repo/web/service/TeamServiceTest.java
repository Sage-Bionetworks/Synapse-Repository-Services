package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;

public class TeamServiceTest {
	
	private TeamServiceImpl teamService = new TeamServiceImpl();
	
	private TeamManager mockTeamManager;
	private PrincipalPrefixDAO mockPrincipalPrefixDAO;
	
	private Team team = null;
	private TeamMember member = null;
	
	@Before
	public void before() throws Exception {
		mockTeamManager = mock(TeamManager.class);
		mockPrincipalPrefixDAO = mock(PrincipalPrefixDAO.class);
			
		team = new Team();
		team.setId("101");
		team.setName("foo bar");
		member = new TeamMember();
		UserGroupHeader ugh = new UserGroupHeader();
		ugh.setUserName("John Smith");
		member.setMember(ugh);

		Map<Team, Collection<TeamMember>> universe = new HashMap<Team, Collection<TeamMember>>();
		universe.put(team, Arrays.asList(new TeamMember[]{member}));
		when(mockTeamManager.listAllTeamsAndMembers()).thenReturn(universe);
		
		PaginatedResults<TeamMember> members = new PaginatedResults<TeamMember>(Arrays.asList(new TeamMember[]{member}), 1);
		when(mockTeamManager.listMembers(eq("101"), anyLong(), anyLong())).thenReturn(members);
		teamService.setTeamManager(mockTeamManager);
		teamService.setPrincipalPrefixDAO(mockPrincipalPrefixDAO);
	}
	
	@Test
	public void testGetTeamsByFragment() throws Exception {
		
		when(mockPrincipalPrefixDAO.listTeamsForPrefix("foo", 1L, 0L)).thenReturn(Arrays.asList(99L));
		when(mockPrincipalPrefixDAO.countTeamsForPrefix("foo")).thenReturn(1L);
		when(mockPrincipalPrefixDAO.listTeamsForPrefix("ba", 1L, 0L)).thenReturn(Arrays.asList(99L));
		when(mockPrincipalPrefixDAO.countTeamsForPrefix("ba")).thenReturn(1L);
		when(mockPrincipalPrefixDAO.listTeamsForPrefix("bas", 1L, 0L)).thenReturn(new LinkedList<Long>());
		when(mockPrincipalPrefixDAO.countTeamsForPrefix("bas")).thenReturn(0L);
		
		List<Team> expected = new ArrayList<Team>(); expected.add(team);
		ListWrapper<Team> wrapped = new ListWrapper<Team>();
		wrapped.setList(expected);
		when(mockTeamManager.list(any(List.class))).thenReturn(wrapped);
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
		Long teamId = 101L;
		when(mockPrincipalPrefixDAO.listTeamMembersForPrefix("Smith", teamId, 1L, 0L)).thenReturn(Arrays.asList(99L));
		when(mockPrincipalPrefixDAO.countTeamMembersForPrefix("Smith", teamId)).thenReturn(1L);
		when(mockPrincipalPrefixDAO.listTeamMembersForPrefix("john", teamId, 1L, 0L)).thenReturn(Arrays.asList(99L));
		when(mockPrincipalPrefixDAO.countTeamMembersForPrefix("john", teamId)).thenReturn(1L);
		when(mockPrincipalPrefixDAO.listTeamMembersForPrefix("bas", teamId, 1L, 0L)).thenReturn(new LinkedList<Long>());
		when(mockPrincipalPrefixDAO.countTeamMembersForPrefix("bas", teamId)).thenReturn(0L);
		List<TeamMember> expected = new ArrayList<TeamMember>();
		expected.add(member);
		ListWrapper<TeamMember> wrapper = new ListWrapper<TeamMember>();
		wrapper.setList(expected);
		when(mockTeamManager.listMembers(any(List.class), any(List.class))).thenReturn(wrapper);
		// test last name match
		PaginatedResults<TeamMember> pr = teamService.getMembers("101", "Smith", 1, 0);
		assertEquals(expected.size(), pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		// test first name match, different case
		pr = teamService.getMembers("101", "john", 1, 0);
		assertEquals(expected.size(), pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		// no match
		pr = teamService.getMembers("101", "bas", 1, 0);
		assertEquals(0, pr.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetTeamNoFragment() throws Exception {
		teamService.get(null, 1, 0);
		verify(mockTeamManager).list(1, 0);
	}
	
	@Test
	public void testGetTeamMemberNoFragment() throws Exception {
		PaginatedResults<TeamMember>results = new PaginatedResults<TeamMember>();
		results.setResults(new ArrayList<TeamMember>());
		when(mockTeamManager.listMembers("101", 1, 0)).thenReturn(results);
		teamService.getMembers("101", null, 1, 0);
		verify(mockTeamManager).listMembers("101", 1, 0);
	}
	

}
