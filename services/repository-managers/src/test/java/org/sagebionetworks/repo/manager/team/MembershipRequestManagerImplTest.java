package org.sagebionetworks.repo.manager.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_DISPLAY_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_ONE_CLICK_JOIN;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_REQUESTER_MESSAGE;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.util.SignedTokenUtil;
import org.sagebionetworks.util.SerializationUtils;

public class MembershipRequestManagerImplTest {
	
	private AuthorizationManager mockAuthorizationManager;
	private MembershipRequestManagerImpl membershipRequestManagerImpl;
	private MembershipRqstSubmissionDAO mockMembershipRqstSubmissionDAO;
	private UserProfileManager mockUserProfileManager;
	private TeamDAO mockTeamDAO;
	private AccessRequirementDAO mockAccessRequirementDAO;
	
	private UserInfo userInfo = null;
	private UserInfo adminInfo = null;
	private static final String TEAM_ID = "111";
	private static final String MEMBER_PRINCIPAL_ID = "999";

	@Before
	public void setUp() throws Exception {
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockMembershipRqstSubmissionDAO = Mockito.mock(MembershipRqstSubmissionDAO.class);
		mockUserProfileManager = Mockito.mock(UserProfileManager.class);
		mockTeamDAO = Mockito.mock(TeamDAO.class);
		mockAccessRequirementDAO = Mockito.mock(AccessRequirementDAO.class);
		membershipRequestManagerImpl = new MembershipRequestManagerImpl(
				mockAuthorizationManager,
				mockMembershipRqstSubmissionDAO,
				mockUserProfileManager,
				mockTeamDAO,
				mockAccessRequirementDAO
				);
		userInfo = new UserInfo(false);
		userInfo.setId(Long.parseLong(MEMBER_PRINCIPAL_ID));
		userInfo.setGroups(Collections.singleton(Long.parseLong(MEMBER_PRINCIPAL_ID)));
		// admin
		adminInfo = new UserInfo(true);
		adminInfo.setId(-1l);
	}
	
	private void validateForCreateExpectFailure(MembershipRqstSubmission mrs, UserInfo userInfo) {
		try {
			MembershipRequestManagerImpl.validateForCreate(mrs, userInfo);
			fail("InvalidModelException expected");
		} catch (InvalidModelException e) {
			// as expected
		}		
	}

	@Test
	public void testValidateForCreate() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		
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
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
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
	
	@Test(expected=UnauthorizedException.class)
	public void testAnonymousCreate() throws Exception {
		UserInfo anonymousInfo = new UserInfo(false);
		anonymousInfo.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		membershipRequestManagerImpl.create(anonymousInfo, mrs);
	}
	
	@Test
	public void testCreate() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId(TEAM_ID);
		when(mockMembershipRqstSubmissionDAO.create((MembershipRqstSubmission)any())).thenReturn(mrs);
		assertEquals(mrs, membershipRequestManagerImpl.create(userInfo, mrs));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateHasUnmetAccessRequirements() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId(TEAM_ID);
		when(mockMembershipRqstSubmissionDAO.create((MembershipRqstSubmission)any())).thenReturn(mrs);
		// now mock an unmet access requirement
		when(mockAccessRequirementDAO.unmetAccessRequirements(
				eq(Collections.singletonList(TEAM_ID)), 
				eq(RestrictableObjectType.TEAM), 
				eq(userInfo.getGroups()), 
				eq(Collections.singletonList(ACCESS_TYPE.PARTICIPATE))))
			.thenReturn(Collections.singletonList(77L));
		// should throw UnauthorizedException
		membershipRequestManagerImpl.create(userInfo, mrs);
	}
	
	@Test
	public void testCreateMembershipRequestNotification() throws Exception {
		List<String> teamAdmins = Arrays.asList(new String[]{"222", "333"});
		when(mockTeamDAO.getAdminTeamMembers(TEAM_ID)).thenReturn(teamAdmins);
		UserProfile up = new UserProfile();
		up.setUserName("auser");
		when(mockUserProfileManager.getUserProfile(userInfo.getId().toString())).thenReturn(up);
		Team team = new Team();
		team.setName("test-team");
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		
		String acceptRequestEndpoint = "https://synapse.org/#acceptRequestEndpoint:";
		String notificationUnsubscribeEndpoint = "https://synapse.org/#notificationUnsubscribeEndpoint:";
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
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
			assertEquals("someone has requested to join your team", result.getMetadata().getSubject());
			assertEquals(Collections.singleton(teamAdmins.get(i)), result.getMetadata().getRecipients());

			// this will give us nine pieces...
			List<String> delims = Arrays.asList(new String[] {
					TEMPLATE_KEY_DISPLAY_NAME,
					TEMPLATE_KEY_TEAM_NAME,
					TEMPLATE_KEY_REQUESTER_MESSAGE,
					TEMPLATE_KEY_ONE_CLICK_JOIN
			});
			List<String> templatePieces = EmailParseUtil.splitEmailTemplate(MembershipRequestManagerImpl.TEAM_MEMBERSHIP_REQUEST_CREATED_TEMPLATE, delims);

			assertTrue(result.getBody().startsWith(templatePieces.get(0)));
			assertTrue(result.getBody().indexOf(templatePieces.get(2))>0);
			String displayName = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(0), templatePieces.get(2));
			assertEquals("auser", displayName);
			assertTrue(result.getBody().indexOf(templatePieces.get(4))>0);
			String teamName = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(2), templatePieces.get(4));
			assertEquals("test-team", teamName);
			assertTrue(result.getBody().indexOf(templatePieces.get(6))>0);
			String inviterMessage = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(4), templatePieces.get(6));
			assertTrue(inviterMessage.indexOf("Please let me in your team.")>=0);
			assertTrue(result.getBody().endsWith(templatePieces.get(8)));
			String acceptRequestToken = 
					EmailParseUtil.getTokenFromString(result.getBody(), 
					templatePieces.get(6)+acceptRequestEndpoint, templatePieces.get(8));
			JoinTeamSignedToken jtst = SerializationUtils.hexDecodeAndDeserialize(acceptRequestToken, JoinTeamSignedToken.class);
			SignedTokenUtil.validateToken(jtst);
			assertEquals(TEAM_ID, jtst.getTeamId());
			assertEquals(MEMBER_PRINCIPAL_ID, jtst.getMemberId());
			assertEquals(teamAdmins.get(i), jtst.getUserId());
		}

	}
	
	@Test
	public void testGet() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		when(mockMembershipRqstSubmissionDAO.get(anyString())).thenReturn(mrs);
		assertEquals(mrs, membershipRequestManagerImpl.get(userInfo, "001"));
		
		// ok to get for another user, if you are an admin
		mrs.setUserId("-1");
		assertEquals(mrs, membershipRequestManagerImpl.get(adminInfo, "001"));
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetForAnotherUser() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId("-1");
		when(mockMembershipRqstSubmissionDAO.get(anyString())).thenReturn(mrs);
		assertEquals(mrs, membershipRequestManagerImpl.get(userInfo, "001"));
	}

	@Test
	public void testDelete() throws Exception {
		String MRS_ID = "222";
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId(MEMBER_PRINCIPAL_ID);
		mrs.setId(MRS_ID);
		when(mockMembershipRqstSubmissionDAO.get(MRS_ID)).thenReturn(mrs);
		membershipRequestManagerImpl.delete(userInfo, MRS_ID);
		Mockito.verify(mockMembershipRqstSubmissionDAO).delete(MRS_ID);
		
		// ok to delete if you are an admin
		mrs.setUserId("333");
		membershipRequestManagerImpl.delete(adminInfo, MRS_ID);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testDeleteOther() throws Exception {
		String MRS_ID = "222";
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId(TEAM_ID);
		mrs.setUserId("333");
		mrs.setId(MRS_ID);
		when(mockMembershipRqstSubmissionDAO.get(MRS_ID)).thenReturn(mrs);
		membershipRequestManagerImpl.delete(userInfo, MRS_ID);
		Mockito.verify(mockMembershipRqstSubmissionDAO).delete(MRS_ID);
	}

	@Test
	public void testGetOpenByTeam() throws Exception {
		MembershipRequest mr = new MembershipRequest();
		mr.setTeamId(TEAM_ID);
		mr.setUserId("333");
		long teamId = 101L;
		List<MembershipRequest> expected = Arrays.asList(new MembershipRequest[]{mr});
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamInRange(eq(teamId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamCount(eq(teamId), anyLong())).thenReturn((long)expected.size());
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		PaginatedResults<MembershipRequest> actual = membershipRequestManagerImpl.getOpenByTeamInRange(userInfo, ""+teamId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetOpenByTeamUnauthorized() throws Exception {
		long teamId = 101L;
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		membershipRequestManagerImpl.getOpenByTeamInRange(userInfo, ""+teamId,1,0);
	}
	
	@Test
	public void testGetOpenByTeamAndRequester() throws Exception {
		MembershipRequest mr = new MembershipRequest();
		mr.setTeamId(TEAM_ID);
		long userId = 333L;
		mr.setUserId(""+userId);
		long teamId = 101L;
		List<MembershipRequest> expected = Arrays.asList(new MembershipRequest[]{mr});
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterInRange(eq(teamId), eq(userId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(teamId), eq(userId), anyLong())).thenReturn((long)expected.size());
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		PaginatedResults<MembershipRequest> actual = membershipRequestManagerImpl.getOpenByTeamAndRequesterInRange(userInfo, ""+teamId,""+userId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetOpenByTeamAndRequesterUnauthorized() throws Exception {
		long userId = 333L;
		long teamId = 101L;
		when(mockAuthorizationManager.canAccess(userInfo, ""+teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		membershipRequestManagerImpl.getOpenByTeamAndRequesterInRange(userInfo, ""+teamId,""+userId,1,0);
	}

	@Test
	public void testGetOpenSubmissionsByRequester() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		mrs.setTeamId(TEAM_ID);
		long userId = userInfo.getId();
		mrs.setUserId(""+userId);
		List<MembershipRqstSubmission> expected = Arrays.asList(new MembershipRqstSubmission[]{mrs});
		when(mockMembershipRqstSubmissionDAO.getOpenSubmissionsByRequesterInRange(eq(userId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRqstSubmissionDAO.getOpenByRequesterCount(eq(userId), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipRqstSubmission> actual = membershipRequestManagerImpl.getOpenSubmissionsByRequesterInRange(userInfo, ""+userId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetOpenSubmissionsByRequesterUnauthorized() throws Exception {
		long userId = userInfo.getId();
		membershipRequestManagerImpl.getOpenSubmissionsByRequesterInRange(userInfo, ""+(userId+999),1,0);
	}
	
	@Test
	public void testGetOpenSubmissionsByRequesterAndTeam() throws Exception {
		MembershipRqstSubmission mrs = new MembershipRqstSubmission();
		long teamId = 111L;
		mrs.setTeamId(""+teamId);
		long userId = userInfo.getId();
		mrs.setUserId(""+userId);
		List<MembershipRqstSubmission> expected = Arrays.asList(new MembershipRqstSubmission[]{mrs});
		when(mockMembershipRqstSubmissionDAO.getOpenSubmissionsByTeamAndRequesterInRange(eq(teamId), eq(userId), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(teamId), eq(userId), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipRqstSubmission> actual = membershipRequestManagerImpl.getOpenSubmissionsByTeamAndRequesterInRange(userInfo, ""+teamId, ""+userId,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetOpenSubmissionsByRequesterAndTeamUnauthorized() throws Exception {
		long teamId = 111L;
		long userId = userInfo.getId();
		membershipRequestManagerImpl.getOpenSubmissionsByTeamAndRequesterInRange(userInfo, ""+teamId, ""+(userId+999),1,0);

	}

}
