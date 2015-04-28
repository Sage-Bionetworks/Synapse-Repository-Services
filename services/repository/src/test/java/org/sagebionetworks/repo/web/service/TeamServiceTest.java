package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.util.Pair;

public class TeamServiceTest {
	
	private TeamServiceImpl teamService;
	private UserManager mockUserManager;
	private TeamManager mockTeamManager;
	private PrincipalPrefixDAO mockPrincipalPrefixDAO;
	private NotificationManager mockNotificationManager;
	
	private Team team = null;
	private TeamMember member = null;
	
	@Before
	public void before() throws Exception {
		mockUserManager = Mockito.mock(UserManager.class);
		
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

		mockNotificationManager = Mockito.mock(NotificationManager.class);
		
		teamService = new TeamServiceImpl(mockTeamManager, mockPrincipalPrefixDAO, mockUserManager, mockNotificationManager);
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
	
	@Test
	public void testAddMember() throws Exception {
		Long userId = 111L;
		String teamId = "222";
		Long principalId = 333L;
		UserInfo userInfo1 = new UserInfo(false); userInfo1.setId(userId);
		UserInfo userInfo2 = new UserInfo(false); userInfo2.setId(principalId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo1);
		when(mockUserManager.getUserInfo(principalId)).thenReturn(userInfo2);
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(Collections.singleton(principalId.toString()));
		String content = "foo";
		Pair<MessageToUser, String> result = new Pair<MessageToUser, String>(mtu, content);
		when(mockTeamManager.createJoinedTeamNotification(userInfo1, userInfo2, teamId)).thenReturn(result);
		teamService.addMember(userId, teamId, principalId.toString());
		verify(mockTeamManager, times(1)).addMember(userInfo1, teamId, userInfo2);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockUserManager).getUserInfo(principalId);
				
		ArgumentCaptor<MessageToUser> mtuArg = ArgumentCaptor.forClass(MessageToUser.class);		
		ArgumentCaptor<String> contentArg = ArgumentCaptor.forClass(String.class);
		verify(mockNotificationManager, times(1)).
			sendNotification(eq(userInfo1), mtuArg.capture(), contentArg.capture());
		assertEquals(mtu, mtuArg.getValue());
		assertEquals(content, contentArg.getValue());
	}
	

}
