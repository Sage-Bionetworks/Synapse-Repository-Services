package org.sagebionetworks.repo.manager.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.dataaccess.RestrictionInformationManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRequestDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamState;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

@ExtendWith(MockitoExtension.class)
public class MembershipRequestManagerImplTest {
	
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private MembershipRequestDAO mockMembershipRequestDAO;
	@Mock
	private UserProfileManager mockUserProfileManager;
	@Mock
	private TeamDAO mockTeamDAO;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDAO;
	@Mock
	private TokenGenerator mockTokenGenerator;
	@Mock
	private RestrictionInformationManager mockRestrictionInformationManager;
	@InjectMocks
	private MembershipRequestManagerImpl membershipRequestManagerImpl;
	
	private UserInfo userInfo = null;
	private UserInfo adminInfo = null;
	private static final String TEAM_ID = "111";
	private static final String MEMBER_PRINCIPAL_ID = "999";
	private RestrictionInformationRequest restrictionInfoRqst;
	private RestrictionInformationResponse noUnmetAccessRqmtResponse;
	private RestrictionInformationResponse hasUnmetAccessRqmtResponse;

	@BeforeEach
	public void setUp() throws Exception {
		userInfo = new UserInfo(false);
		userInfo.setId(Long.parseLong(MEMBER_PRINCIPAL_ID));
		userInfo.setGroups(Collections.singleton(Long.parseLong(MEMBER_PRINCIPAL_ID)));
		// admin
		adminInfo = new UserInfo(true);
		adminInfo.setId(-1l);
		restrictionInfoRqst = new RestrictionInformationRequest();
		restrictionInfoRqst.setObjectId(TEAM_ID);
		restrictionInfoRqst.setRestrictableObjectType(RestrictableObjectType.TEAM);
		noUnmetAccessRqmtResponse = new RestrictionInformationResponse();
		noUnmetAccessRqmtResponse.setHasUnmetAccessRequirement(false);
		hasUnmetAccessRqmtResponse = new RestrictionInformationResponse();
		hasUnmetAccessRqmtResponse.setHasUnmetAccessRequirement(true);

	}
	
	private void validateForCreateExpectFailure(MembershipRequest mrs, UserInfo userInfo) {
		Assertions.assertThrows(InvalidModelException.class, ()-> {
			MembershipRequestManagerImpl.validateForCreate(mrs, userInfo);
		});
	}

	@Test
	public void testValidateForCreate() throws Exception {
		MembershipRequest mrs = new MembershipRequest();
		
		// Happy case
		mrs.setTeamId("101");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		MembershipRequestManagerImpl.validateForCreate(mrs, userInfo);
		
		// try to request for someone else, as an admin
		mrs.setTeamId("101");
		mrs.setUserId("102");
		MembershipRequestManagerImpl.validateForCreate(mrs, adminInfo);
		
		// try to request for someone else
		mrs.setTeamId("101");
		mrs.setUserId("102");
		validateForCreateExpectFailure(mrs, userInfo);

		// can't set createdBy
		mrs.setTeamId("101");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setCreatedBy("me");
		validateForCreateExpectFailure(mrs, userInfo);

		// can't set createdOn
		mrs.setTeamId("101");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setCreatedBy(null);
		mrs.setCreatedOn(new Date());
		validateForCreateExpectFailure(mrs, userInfo);

		// must set Team
		mrs.setTeamId(null);
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setCreatedBy(null);
		mrs.setCreatedOn(null);
		validateForCreateExpectFailure(mrs, userInfo);

		// can't set id
		mrs.setTeamId("101");
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setCreatedBy(null);
		mrs.setCreatedOn(null);
		mrs.setId("007");
		validateForCreateExpectFailure(mrs, userInfo);

	}
	
	@Test
	public void testPopulateCreationFields() throws Exception {
		MembershipRequest mrs = new MembershipRequest();
		Date now = new Date();
		MembershipRequestManagerImpl.populateCreationFields(userInfo, mrs, now);
		assertEquals(MEMBER_PRINCIPAL_ID, mrs.getCreatedBy());
		assertEquals(now, mrs.getCreatedOn());
		assertEquals(MEMBER_PRINCIPAL_ID, mrs.getUserId());
		
		// don't override userId if already set
		mrs.setUserId("something else");
		MembershipRequestManagerImpl.populateCreationFields(userInfo, mrs, now);
		assertEquals("something else", mrs.getUserId());
	}
	
	@Test
	public void testAnonymousCreate() throws Exception {
		UserInfo anonymousInfo = new UserInfo(false);
		anonymousInfo.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		MembershipRequest mrs = new MembershipRequest();
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipRequestManagerImpl.create(anonymousInfo, mrs);
		});
	}
	
	@Test
	public void testCreate() throws Exception {
		when(mockRestrictionInformationManager.
				getRestrictionInformation(userInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		when(mockMembershipRequestDAO.create((MembershipRequest)any())).thenReturn(mrs);
		when(mockTeamDAO.getState(TEAM_ID)).thenReturn(TeamState.OPEN);
		assertEquals(mrs, membershipRequestManagerImpl.create(userInfo, mrs));
	}
	
	@Test
	public void testCreateHasUnmetAccessRequirements() throws Exception {
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(userInfo, restrictionInfoRqst)).
					thenReturn(hasUnmetAccessRqmtResponse);
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			// should throw UnauthorizedException
			membershipRequestManagerImpl.create(userInfo, mrs);
		});
	}
	
	@Test
	public void testCreateMembershipRequestNotification() throws Exception {
		List<String> teamAdmins = Arrays.asList(new String[]{"222", "333"});
		when(mockTeamDAO.getAdminTeamMemberIds(TEAM_ID)).thenReturn(teamAdmins);
		UserProfile up = new UserProfile();
		up.setUserName("auser");
		when(mockUserProfileManager.getUserProfile(userInfo.getId().toString())).thenReturn(up);
		Team team = new Team();
		team.setName("test-team");
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		
		String acceptRequestEndpoint = "https://synapse.org/#acceptRequestEndpoint:";
		String notificationUnsubscribeEndpoint = "https://synapse.org/#notificationUnsubscribeEndpoint:";
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		mrs.setCreatedBy(MEMBER_PRINCIPAL_ID);
		mrs.setMessage("Please let me in your team.");
		List<MessageToUserAndBody> resultList = membershipRequestManagerImpl.
				createMembershipRequestNotification(mrs,
						acceptRequestEndpoint,
						notificationUnsubscribeEndpoint);
		assertEquals(teamAdmins.size(), resultList.size());
		for (int i=0; i<resultList.size(); i++) {
			MessageToUserAndBody result = resultList.get(i);
			assertEquals("Someone Has Requested to Join Your Team", result.getMetadata().getSubject());
			assertEquals(Collections.singleton(teamAdmins.get(i)), result.getMetadata().getRecipients());
			String userId = MEMBER_PRINCIPAL_ID;
			String displayName = "auser";
			String teamId = TEAM_ID;
			String teamName = "test-team";
			String requesterMessage = "The requester sends the following message: <Blockquote> Please let me in your team. </Blockquote> ";
			String adminId = teamAdmins.get(i);
			String oneClickJoin = EmailUtils.createOneClickJoinTeamLink(acceptRequestEndpoint, adminId, userId, teamId, mrs.getCreatedOn(), mockTokenGenerator);
			String expected = "<html style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-size: 10px;-webkit-tap-highlight-color: rgba(0, 0, 0, 0);\">\r\n" + 
					"  <body style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-family: &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif;font-size: 14px;line-height: 1.42857143;color: #333333;background-color: #ffffff;\">\r\n" + 
					"    <div style=\"margin: 10px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;\">\r\n" + 
					"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;margin-bottom: 20px;font-size: 16px;font-weight: 300;line-height: 1.4;\">Hello,</p>\r\n" + 
					"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
					"        <strong style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-weight: bold;\"><a href=\"https://www.synapse.org/#!Profile:" + userId + "\">" + displayName + "</a></strong>\r\n" + 
					"        has requested to join team\r\n" + 
					"        <strong style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-weight: bold;\"><a href=\"https://www.synapse.org/#!Team:" + teamId + "\">" + teamName + "</a></strong>.\r\n" + 
					"      </p>\r\n" + 
					"      <blockquote style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;padding: 10px 20px;margin: 0 0 20px;font-size: 17.5px;border-left: 5px solid #eeeeee;\">\r\n" + 
					"        " + requesterMessage + "\r\n" + 
					"      </blockquote>\r\n" + 
					"      <a href=\"" + oneClickJoin + "\" style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;background-color: #337ab7;color: #ffffff;text-decoration: none;display: inline-block;margin-bottom: 0;font-weight: normal;text-align: center;vertical-align: middle;-ms-touch-action: manipulation;touch-action: manipulation;cursor: pointer;background-image: none;border: 1px solid transparent;white-space: nowrap;padding: 10px 16px;font-size: 18px;line-height: 1.3333333;border-radius: 6px;-webkit-user-select: none;-moz-user-select: none;-ms-user-select: none;user-select: none;border-color: #2e6da4;\">Accept this Request!</a>\r\n" + 
					"      <p style=\"margin-top: 10px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">If you do not wish to accept this request, please disregard this message.</p>\r\n" + 
					"      <br style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;\">\r\n" + 
					"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
					"        Sincerely,\r\n" + 
					"      </p>\r\n" + 
					"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
					"        <img src=\"https://s3.amazonaws.com/static.synapse.org/images/SynapseLogo2.png\" style=\"display: inline;width: 40px;height: 40px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;border: 0;vertical-align: middle;\"> Synapse Administration\r\n" + 
					"      </p>\r\n" + 
					"    </div>\r\n" + 
					"  </body>\r\n" + 
					"</html>\r\n";
			assertEquals(expected, result.getBody());
		}
	}
	
	@Test
	public void testGet() throws Exception {
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		when(mockMembershipRequestDAO.get(anyString())).thenReturn(mrs);
		when(mockAuthorizationManager.canAccessMembershipRequest(userInfo, mrs, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		assertEquals(mrs, membershipRequestManagerImpl.get(userInfo, "001"));
		
		// ok to get for another user, if you are an admin
		mrs.setUserId("-1");
		when(mockAuthorizationManager.canAccessMembershipRequest(adminInfo, mrs, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		assertEquals(mrs, membershipRequestManagerImpl.get(adminInfo, "001"));
	}

	@Test
	public void testGetForAnotherUser() throws Exception {
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId("-1");
		when(mockMembershipRequestDAO.get(anyString())).thenReturn(mrs);
		when(mockAuthorizationManager.canAccessMembershipRequest(userInfo, mrs, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipRequestManagerImpl.get(userInfo, "001");
		});
	}

	@Test
	public void testDelete() throws Exception {
		String MRS_ID = "222";
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setId(MRS_ID);
		when(mockMembershipRequestDAO.get(MRS_ID)).thenReturn(mrs);
		when(mockAuthorizationManager.canAccessMembershipRequest(userInfo, mrs, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.authorized());
		membershipRequestManagerImpl.delete(userInfo, MRS_ID);
		Mockito.verify(mockMembershipRequestDAO).delete(MRS_ID);
		
		// ok to delete if you are an admin
		mrs.setUserId("333");
		when(mockAuthorizationManager.canAccessMembershipRequest(adminInfo, mrs, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.authorized());
		membershipRequestManagerImpl.delete(adminInfo, MRS_ID);
	}
	
	@Test
	public void testDeleteOther() throws Exception {
		String MRS_ID = "222";
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId("333");
		mrs.setId(MRS_ID);
		when(mockMembershipRequestDAO.get(MRS_ID)).thenReturn(mrs);
		when(mockAuthorizationManager.canAccessMembershipRequest(userInfo, mrs, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipRequestManagerImpl.delete(userInfo, MRS_ID);
		});
	}

	@Test
	public void testGetOpenByTeam() throws Exception {
		MembershipRequest mr = new MembershipRequest();
		mr.setTeamId(TEAM_ID);
		mr.setUserId("333");
		long teamId = 101L;
		List<MembershipRequest> expected = Arrays.asList(new MembershipRequest[]{mr});
		when(mockMembershipRequestDAO.getOpenByTeamInRange(eq(teamId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRequestDAO.getOpenByTeamCount(eq(teamId), anyLong())).thenReturn((long)expected.size());
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		PaginatedResults<MembershipRequest> actual = membershipRequestManagerImpl.getOpenByTeamInRange(userInfo, ""+teamId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetOpenByTeamUnauthorized() throws Exception {
		long teamId = 101L;
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipRequestManagerImpl.getOpenByTeamInRange(userInfo, ""+teamId,1,0);
		});
	}
	
	@Test
	public void testGetOpenByTeamAndRequester() throws Exception {
		MembershipRequest mr = new MembershipRequest();
		mr.setTeamId(TEAM_ID);
		long userId = 333L;
		mr.setUserId(""+userId);
		long teamId = 101L;
		List<MembershipRequest> expected = Arrays.asList(new MembershipRequest[]{mr});
		when(mockMembershipRequestDAO.getOpenByTeamAndRequesterInRange(eq(teamId), eq(userId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRequestDAO.getOpenByTeamAndRequesterCount(eq(teamId), eq(userId), anyLong())).thenReturn((long)expected.size());
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		PaginatedResults<MembershipRequest> actual = membershipRequestManagerImpl.getOpenByTeamAndRequesterInRange(userInfo, ""+teamId,""+userId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test
	public void testGetOpenByTeamAndRequesterUnauthorized() throws Exception {
		long userId = 333L;
		long teamId = 101L;
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipRequestManagerImpl.getOpenByTeamAndRequesterInRange(userInfo, ""+teamId,""+userId,1,0);
		});
	}

	@Test
	public void testGetOpenSubmissionsByRequester() throws Exception {
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		long userId = userInfo.getId();
		mrs.setUserId(""+userId);
		List<MembershipRequest> expected = Arrays.asList(new MembershipRequest[]{mrs});
		when(mockMembershipRequestDAO.getOpenByRequesterInRange(eq(userId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRequestDAO.getOpenByRequesterCount(eq(userId), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipRequest> actual = membershipRequestManagerImpl.getOpenSubmissionsByRequesterInRange(userInfo, ""+userId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetOpenSubmissionsByRequesterUnauthorized() throws Exception {
		long userId = userInfo.getId();
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipRequestManagerImpl.getOpenSubmissionsByRequesterInRange(userInfo, ""+(userId+999),1,0);
		});
	}
	
	@Test
	public void testGetOpenSubmissionsByRequesterAndTeam() throws Exception {
		MembershipRequest mrs = new MembershipRequest();
		long teamId = 111L;
		mrs.setTeamId(""+teamId);
		long userId = userInfo.getId();
		mrs.setUserId(""+userId);
		List<MembershipRequest> expected = Arrays.asList(new MembershipRequest[]{mrs});
		when(mockMembershipRequestDAO.getOpenByTeamAndRequesterInRange(eq(teamId), eq(userId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRequestDAO.getOpenByTeamAndRequesterCount(eq(teamId), eq(userId), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipRequest> actual = membershipRequestManagerImpl.getOpenSubmissionsByTeamAndRequesterInRange(userInfo, ""+teamId, ""+userId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test
	public void testGetOpenSubmissionsByRequesterAndTeamUnauthorized() throws Exception {
		long teamId = 111L;
		long userId = userInfo.getId();
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipRequestManagerImpl.getOpenSubmissionsByTeamAndRequesterInRange(userInfo, ""+teamId, ""+(userId+999),1,0);
		});
	}

	@Test
	public void testGetOpenSubmissionsCountForTeamAdminWithNullUserInfo() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			membershipRequestManagerImpl.getOpenSubmissionsCountForTeamAdmin(null);
		});
	}

	@Test
	public void testGetOpenSubmissionsCountForTeamAdminWithEmptyTeams() {
		when(mockTeamDAO.getAllTeamsUserIsAdmin(MEMBER_PRINCIPAL_ID)).thenReturn(new LinkedList<String>());
		Count result = membershipRequestManagerImpl.getOpenSubmissionsCountForTeamAdmin(userInfo);
		assertNotNull(result);
		assertEquals((Long)0L, result.getCount());
		verify(mockMembershipRequestDAO, never()).getOpenByTeamsCount(anyList(), anyLong());
	}

	@Test
	public void testGetOpenSubmissionsCountForTeamAdmin() {
		Long count = 7L;
		List<String> teams = Arrays.asList("1");
		when(mockTeamDAO.getAllTeamsUserIsAdmin(MEMBER_PRINCIPAL_ID)).thenReturn(teams);
		when(mockMembershipRequestDAO.getOpenByTeamsCount(eq(teams), anyLong())).thenReturn(count);
		Count result = membershipRequestManagerImpl.getOpenSubmissionsCountForTeamAdmin(userInfo);
		assertNotNull(result);
		assertEquals(count, result.getCount());
	}

	@Test
	public void testCreateRequestPublicTeam() {
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		when(mockTeamDAO.getState(mrs.getTeamId())).thenReturn(TeamState.PUBLIC);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(userInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);
		
		String errorMessage = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			membershipRequestManagerImpl.create(userInfo, mrs);
		}).getMessage();
		assertEquals("This team is already open for the public to join, membership requests are not needed.", errorMessage);

	}

	@Test
	public void testCreateRequestTeamClosedToMembershipRequests() {
		MembershipRequest mrs = new MembershipRequest();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		when(mockTeamDAO.getState(mrs.getTeamId())).thenReturn(TeamState.CLOSED);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(userInfo, restrictionInfoRqst)).
				thenReturn(noUnmetAccessRqmtResponse);

		String errorMessage = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			membershipRequestManagerImpl.create(userInfo, mrs);
		}).getMessage();
		assertEquals("This team has been closed to new membership requests.", errorMessage);

	}
}
