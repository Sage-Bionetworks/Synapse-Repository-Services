package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TeamServiceTest {
	
	private TeamServiceImpl teamService;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private TeamManager mockTeamManager;
	@Mock
	private PrincipalPrefixDAO mockPrincipalPrefixDAO;
	@Mock
	private NotificationManager mockNotificationManager;
	@Mock
	private UserProfileManager mockUserProfileManager;
	@Mock
	private TokenGenerator mockTokenGenerator;
	
	private Team team = null;
	private TeamMember member = null;
	
	@Before
	public void before() throws Exception {
			
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
		
		PaginatedResults<TeamMember> members = PaginatedResults.createWithLimitAndOffset(Arrays.asList(new TeamMember[]{member}), 100L, 0L);
		when(mockTeamManager.listMembers(eq("101"), anyLong(), anyLong())).thenReturn(members);
		
		teamService = new TeamServiceImpl();
		
		ReflectionTestUtils.setField(teamService, "teamManager", mockTeamManager);
		ReflectionTestUtils.setField(teamService, "principalPrefixDAO", mockPrincipalPrefixDAO);
		ReflectionTestUtils.setField(teamService, "userManager", mockUserManager);
		ReflectionTestUtils.setField(teamService, "notificationManager", mockNotificationManager);
		ReflectionTestUtils.setField(teamService, "userProfileManager", mockUserProfileManager);
		ReflectionTestUtils.setField(teamService, "tokenGenerator", mockTokenGenerator);
	}
	
	@Test
	public void testGetTeamsByFragment() {
		List<Long> listWithTeam = Arrays.asList(99L);
		List<Long> emptyList = new LinkedList<>();

		when(mockPrincipalPrefixDAO.listTeamsForPrefix("foo", 1L, 0L)).thenReturn(listWithTeam);
		when(mockPrincipalPrefixDAO.listTeamsForPrefix("ba", 1L, 0L)).thenReturn(listWithTeam);
		when(mockPrincipalPrefixDAO.listTeamsForPrefix("bas", 1L, 0L)).thenReturn(emptyList);

		List<Team> expected = new ArrayList<Team>(); expected.add(team);
		ListWrapper<Team> wrapped = new ListWrapper<Team>();
		wrapped.setList(expected);
		when(mockTeamManager.list(any(List.class))).thenReturn(wrapped);
		PaginatedResults<Team> pr = teamService.get("foo", 1, 0);
		assertEquals(2L, pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		pr = teamService.get("ba", 1, 0);
		assertEquals(2L, pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		
		// no match
		pr = teamService.get("bas", 1, 0);
		assertEquals(2L, pr.getTotalNumberOfResults());
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
		assertEquals(2L, pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());
		
		assertEquals(1L, teamService.getMemberCount("101", "Smith").getCount().longValue());
		
		// test first name match, different case
		pr = teamService.getMembers("101", "john", 1, 0);
		assertEquals(2L, pr.getTotalNumberOfResults());
		assertEquals(expected, pr.getResults());

		assertEquals(1L, teamService.getMemberCount("101", "john").getCount().longValue());

		// no match
		pr = teamService.getMembers("101", "bas", 1, 0);
		assertEquals(2L, pr.getTotalNumberOfResults());

		assertEquals(0L, teamService.getMemberCount("101", "bas").getCount().longValue());
}
	
	@Test
	public void testGetTeamNoFragment() throws Exception {
		teamService.get(null, 1, 0);
		verify(mockTeamManager).list(1, 0);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetTeamWithUnderMinLimit() {
		teamService.get(null, 0, 0);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetTeamWithOverMaxLimit() {
		teamService.get(null, 51, 0);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetTeamWithNegativeOffset() {
		teamService.get(null, 50, -1);
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
	public void testGetTeamMemberCountNoFragment() throws Exception {
		teamService.getMemberCount("101", null);
		verify(mockTeamManager).countMembers("101");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetTeamMemberWithUnderMinLimit() {
		teamService.getMembers("101", null, 0, 0);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetTeamMemberWithOverMaxLimit() {
		teamService.getMembers("101", null, 51, 0);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetTeamMemberWithNegativeOffset() {
		teamService.getMembers("101", null, 1, -1);
	}
	
	@Test
	public void testAddMember() throws Exception {
		Long userId = 111L;
		String teamId = "222";
		Long principalId = 333L;
		String teamEndpoint = "teamEndpoint:";
		String notificationUnsubscribeEndpoint = "notificationUnsubscribeEndpoint:";
		UserInfo userInfo1 = new UserInfo(false); userInfo1.setId(userId);
		UserInfo userInfo2 = new UserInfo(false); userInfo2.setId(principalId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo1);
		when(mockUserManager.getUserInfo(principalId)).thenReturn(userInfo2);
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(Collections.singleton(principalId.toString()));
		String content = "foo";
		MessageToUserAndBody result = new MessageToUserAndBody(mtu, content, "text/plain");
		List<MessageToUserAndBody> resultList = Collections.singletonList(result);
		when(mockTeamManager.createJoinedTeamNotifications(userInfo1, userInfo2, teamId, teamEndpoint, notificationUnsubscribeEndpoint)).thenReturn(resultList);
		when(mockTeamManager.addMember(userInfo1, teamId, userInfo2)).thenReturn(true);
		
		teamService.addMember(userId, teamId, principalId.toString(), teamEndpoint, notificationUnsubscribeEndpoint);
		verify(mockTeamManager, times(1)).addMember(userInfo1, teamId, userInfo2);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockUserManager).getUserInfo(principalId);
				
		ArgumentCaptor<List> messageArg = ArgumentCaptor.forClass(List.class);
		verify(mockNotificationManager).
			sendNotifications(eq(userInfo1), messageArg.capture());
		assertEquals(1, messageArg.getValue().size());		
		assertEquals(result, messageArg.getValue().get(0));		

	}
	
	@Test
	public void testAddMemberByJoinTeamSignedToken() throws Exception {
		Long userId = 111L;
		String teamId = "222";
		Long principalId = 333L;
		String teamEndpoint = "teamEndpoint:";
		String notificationUnsubscribeEndpoint = "notificationUnsubscribeEndpoint:";
		UserInfo userInfo1 = new UserInfo(false); userInfo1.setId(userId);
		UserInfo userInfo2 = new UserInfo(false); userInfo2.setId(principalId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo1);
		when(mockUserManager.getUserInfo(principalId)).thenReturn(userInfo2);
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(Collections.singleton(principalId.toString()));
		String content = "foo";
		MessageToUserAndBody result = new MessageToUserAndBody(mtu, content, "text/plain");
		List<MessageToUserAndBody> resultList = Collections.singletonList(result);
		when(mockTeamManager.createJoinedTeamNotifications(userInfo1, userInfo2, teamId, teamEndpoint, notificationUnsubscribeEndpoint)).thenReturn(resultList);
		when(mockTeamManager.get(teamId)).thenReturn(team);
		UserProfile memberUserProfile = new UserProfile();
		memberUserProfile.setUserName("auser");
		when(mockUserProfileManager.getUserProfile(principalId.toString())).thenReturn(memberUserProfile);
		
		JoinTeamSignedToken jtst = new JoinTeamSignedToken();
		jtst.setUserId(userId.toString());
		jtst.setTeamId(teamId);
		jtst.setMemberId(principalId.toString());
		jtst.setHmac("someHMAC");
		
		when(mockTeamManager.addMember(userInfo1, teamId, userInfo2)).thenReturn(true);
		
		ResponseMessage message = teamService.addMember(jtst, teamEndpoint, notificationUnsubscribeEndpoint);
		verify(mockTeamManager, times(1)).addMember(userInfo1, teamId, userInfo2);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockUserManager).getUserInfo(principalId);
				
		ArgumentCaptor<List> messageArg = ArgumentCaptor.forClass(List.class);
		verify(mockNotificationManager).
			sendNotifications(eq(userInfo1), messageArg.capture());
		assertEquals(1, messageArg.getValue().size());		
		assertEquals(result, messageArg.getValue().get(0));	
		
		assertEquals("User auser has been added to team foo bar.", message.getMessage());

	}
	
	@Test
	public void testAddMemberByJoinTeamSignedTokenALREADYInTeam() throws Exception {
		Long userId = 111L;
		String teamId = "222";
		Long principalId = 333L;
		String teamEndpoint = "teamEndpoint:";
		String notificationUnsubscribeEndpoint = "notificationUnsubscribeEndpoint:";
		UserInfo userInfo1 = new UserInfo(false); userInfo1.setId(userId);
		UserInfo userInfo2 = new UserInfo(false); userInfo2.setId(principalId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo1);
		when(mockUserManager.getUserInfo(principalId)).thenReturn(userInfo2);
		MessageToUser mtu = new MessageToUser();
		mtu.setRecipients(Collections.singleton(principalId.toString()));
		String content = "foo";
		MessageToUserAndBody result = new MessageToUserAndBody(mtu, content, "text/plain");
		List<MessageToUserAndBody> resultList = Collections.singletonList(result);
		when(mockTeamManager.createJoinedTeamNotifications(userInfo1, userInfo2, teamId, teamEndpoint, notificationUnsubscribeEndpoint)).thenReturn(resultList);
		when(mockTeamManager.get(teamId)).thenReturn(team);
		UserProfile memberUserProfile = new UserProfile();
		memberUserProfile.setUserName("auser");
		when(mockUserProfileManager.getUserProfile(principalId.toString())).thenReturn(memberUserProfile);
		
		JoinTeamSignedToken jtst = new JoinTeamSignedToken();
		jtst.setUserId(userId.toString());
		jtst.setTeamId(teamId);
		jtst.setMemberId(principalId.toString());
		jtst.setHmac("someHMAC");
		
		when(mockTeamManager.addMember(userInfo1, teamId, userInfo2)).thenReturn(false);
		
		ResponseMessage message = teamService.addMember(jtst, teamEndpoint, notificationUnsubscribeEndpoint);
		verify(mockTeamManager, times(1)).addMember(userInfo1, teamId, userInfo2);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockUserManager).getUserInfo(principalId);
				
		verify(mockNotificationManager, never()).sendNotifications(eq(userInfo1), (List<MessageToUserAndBody>)any(List.class));
		
		assertEquals("User auser is already in team foo bar.", message.getMessage());

	}
	

}
