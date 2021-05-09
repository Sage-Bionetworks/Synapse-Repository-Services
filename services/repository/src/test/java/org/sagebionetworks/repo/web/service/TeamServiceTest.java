package org.sagebionetworks.repo.web.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.sagebionetworks.repo.model.TeamMemberTypeFilterOptions;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.message.MessageToUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTest {
	
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
	@InjectMocks
	private TeamServiceImpl teamService;
	
	private Team team = null;
	private TeamMember member = null;
	
	@BeforeEach
	public void before() throws Exception {
			
		team = new Team();
		team.setId("101");
		team.setName("foo bar");
		member = new TeamMember();
		UserGroupHeader ugh = new UserGroupHeader();
		ugh.setUserName("John Smith");
		member.setMember(ugh);
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
		List<TeamMember> tms1 = Collections.singletonList(member);
		PaginatedResults<TeamMember> tms1paginated = PaginatedResults.createWithLimitAndOffset(tms1, 10L, 0L);
		List<TeamMember> tms2 = Collections.emptyList();
		PaginatedResults<TeamMember> tms2paginated = PaginatedResults.createWithLimitAndOffset(tms2, 10L, 0L);

		Long teamId = 101L;
		when(mockTeamManager.listMembersForPrefix("Smith", teamId.toString(), TeamMemberTypeFilterOptions.ALL,1L, 0L)).thenReturn(tms1paginated);
		when(mockPrincipalPrefixDAO.countTeamMembersForPrefix("Smith", teamId)).thenReturn(1L);
		when(mockTeamManager.listMembersForPrefix("john", teamId.toString(), TeamMemberTypeFilterOptions.ALL,1L, 0L)).thenReturn(tms1paginated);
		when(mockPrincipalPrefixDAO.countTeamMembersForPrefix("john", teamId)).thenReturn(1L);
		when(mockTeamManager.listMembersForPrefix("bas", teamId.toString(), TeamMemberTypeFilterOptions.ALL,1L, 0L)).thenReturn(tms2paginated);
		when(mockPrincipalPrefixDAO.countTeamMembersForPrefix("bas", teamId)).thenReturn(0L);
		
		// test last name match
		PaginatedResults<TeamMember> pr = teamService.getMembers("101", "Smith", TeamMemberTypeFilterOptions.ALL, 1, 0);
		assertEquals(1L, pr.getTotalNumberOfResults());
		assertEquals(tms1, pr.getResults());
		
		assertEquals(1L, teamService.getMemberCount("101", "Smith").getCount().longValue());
		
		// test first name match, different case
		pr = teamService.getMembers("101", "john", TeamMemberTypeFilterOptions.ALL, 1, 0);
		assertEquals(1L, pr.getTotalNumberOfResults());
		assertEquals(tms1, pr.getResults());

		assertEquals(1L, teamService.getMemberCount("101", "john").getCount().longValue());

		// no match
		pr = teamService.getMembers("101", "bas", TeamMemberTypeFilterOptions.ALL,1, 0);
		assertEquals(0L, pr.getTotalNumberOfResults());

		assertEquals(0L, teamService.getMemberCount("101", "bas").getCount().longValue());
}
	
	@Test
	public void testGetTeamNoFragment() throws Exception {
		teamService.get(null, 1, 0);
		verify(mockTeamManager).list(1, 0);
	}

	@Test
	public void testGetTeamWithUnderMinLimit() {
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			teamService.get(null, 0, 0);
		});
		assertEquals("limit must be between 1 and 50", ex.getMessage());
	}

	@Test
	public void testGetTeamWithOverMaxLimit() {
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			teamService.get(null, 51, 0);
		});
		assertEquals("limit must be between 1 and 50", ex.getMessage());
	}

	@Test
	public void testGetTeamWithNegativeOffset() {
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			teamService.get(null, 50, -1);
		});
		assertEquals("'offset' may not be negative", ex.getMessage());
	}

	@Test
	public void testGetMembersWithNullTeamId() {
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			teamService.getMembers(null, null, TeamMemberTypeFilterOptions.ALL, 1, 0);
		});
		assertEquals("The teamId is required.", ex.getMessage());
	}
	
	@Test
	public void testGetTeamMemberNoFragment() throws Exception {
		PaginatedResults<TeamMember>results = new PaginatedResults<TeamMember>();
		results.setResults(new ArrayList<TeamMember>());
		when(mockTeamManager.listMembers("101", TeamMemberTypeFilterOptions.ALL, 1, 0)).thenReturn(results);
		teamService.getMembers("101",null, TeamMemberTypeFilterOptions.ALL, 1, 0);
		verify(mockTeamManager).listMembers("101", TeamMemberTypeFilterOptions.ALL, 1, 0);
	}

	@Test
	public void testGetTeamMemberCountNoFragment() throws Exception {
		teamService.getMemberCount("101", null);
		verify(mockTeamManager).countMembers("101");
	}

	@Test
	public void testGetTeamMemberWithUnderMinLimit() {
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			teamService.getMembers("101", null, TeamMemberTypeFilterOptions.ALL, 0, 0);
		});
		assertEquals("limit must be between 1 and 50", ex.getMessage());
	}

	@Test
	public void testGetTeamMemberWithOverMaxLimit() {
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			teamService.getMembers("101", null, TeamMemberTypeFilterOptions.ALL, 51, 0);
		});
		assertEquals("limit must be between 1 and 50", ex.getMessage());
	}

	@Test
	public void testGetTeamMemberWithNegativeOffset() {
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			teamService.getMembers("101", null, TeamMemberTypeFilterOptions.ALL, 1, -1);
		});
		assertEquals("'offset' may not be negative", ex.getMessage());
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
		verify(mockNotificationManager).sendNotifications(eq(userInfo1), messageArg.capture());
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
		verify(mockNotificationManager).sendNotifications(eq(userInfo1), messageArg.capture());
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
				
		verifyZeroInteractions(mockNotificationManager);
		
		assertEquals("User auser is already in team foo bar.", message.getMessage());

	}
	

}
