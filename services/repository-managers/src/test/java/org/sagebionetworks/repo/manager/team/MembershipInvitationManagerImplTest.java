package org.sagebionetworks.repo.manager.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_INVITER_MESSAGE;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_ONE_CLICK_JOIN;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE;
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
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.util.SignedTokenUtil;
import org.sagebionetworks.util.SerializationUtils;

public class MembershipInvitationManagerImplTest {
	
	private MembershipInvitationManagerImpl membershipInvitationManagerImpl = null;
	private AuthorizationManager mockAuthorizationManager = null;
	private MembershipInvtnSubmissionDAO mockMembershipInvtnSubmissionDAO = null;
	private TeamDAO mockTeamDAO = null;
	
	private UserInfo userInfo = null;

	private static final String MEMBER_PRINCIPAL_ID = "999";

	private static final String TEAM_ID = "123";
	private static final String MIS_ID = "987";
	
	private static MembershipInvtnSubmission createMembershipInvtnSubmission(String id) {
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		mis.setId(id);
		mis.setTeamId(TEAM_ID);
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setMessage("Please join our team.");
		return mis;
	}
	
	private static MembershipInvitation createMembershipInvitation() {
		MembershipInvitation mi = new MembershipInvitation();
		mi.setTeamId(TEAM_ID);
		mi.setUserId(MEMBER_PRINCIPAL_ID);
		return mi;
	}
	
	@Before
	public void setUp() throws Exception {
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockMembershipInvtnSubmissionDAO = Mockito.mock(MembershipInvtnSubmissionDAO.class);
		mockTeamDAO = Mockito.mock(TeamDAO.class);
		membershipInvitationManagerImpl = new MembershipInvitationManagerImpl(
				mockAuthorizationManager,
				mockMembershipInvtnSubmissionDAO,
				mockTeamDAO
				);
		userInfo = new UserInfo(false, MEMBER_PRINCIPAL_ID);
	}

	
	private void validateForCreateExpectFailure(MembershipInvtnSubmission mis) {
		try {
			MembershipInvitationManagerImpl.validateForCreate(mis);
			fail("InvalidModelException expected");
		} catch (InvalidModelException e) {
			// as expected
		}		
	}

	@Test
	public void testValidateForCreate() throws Exception {
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		
		// Happy case
		mis.setTeamId("101");
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		MembershipInvitationManagerImpl.validateForCreate(mis);

		// can't set createdBy
		mis.setTeamId("101");
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setCreatedBy("me");
		validateForCreateExpectFailure(mis);
		
		
		// must set invitees
		mis.setTeamId("101");
		mis.setInviteeId(null);
		mis.setCreatedBy(null);
		validateForCreateExpectFailure(mis);

		// can't set createdOn
		mis.setTeamId("101");
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setCreatedBy(null);
		mis.setCreatedOn(new Date());
		validateForCreateExpectFailure(mis);

		// must set Team
		mis.setTeamId(null);
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setCreatedBy(null);
		mis.setCreatedOn(null);
		validateForCreateExpectFailure(mis);

		// can't set id
		mis.setTeamId("101");
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setCreatedBy(null);
		mis.setCreatedOn(null);
		mis.setId("007");
		validateForCreateExpectFailure(mis);

	}
	
	@Test
	public void testPopulateCreationFields() throws Exception {
		MembershipInvtnSubmission mis = new MembershipInvtnSubmission();
		Date now = new Date();
		MembershipInvitationManagerImpl.populateCreationFields(userInfo, mis, now);
		assertEquals(MEMBER_PRINCIPAL_ID, mis.getCreatedBy());
		assertEquals(now, mis.getCreatedOn());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testNonAdminCreate() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(null);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		membershipInvitationManagerImpl.create(userInfo, mis);
	}
	
	@Test
	public void testAdminCreate() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(null);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		membershipInvitationManagerImpl.create(userInfo, mis);
		Mockito.verify(mockMembershipInvtnSubmissionDAO).create(mis);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testNonAdminGet() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(mockMembershipInvtnSubmissionDAO.get(MIS_ID)).thenReturn(mis);
		membershipInvitationManagerImpl.get(userInfo, MIS_ID);
	}
	
	@Test
	public void testAdminGet() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockMembershipInvtnSubmissionDAO.get(MIS_ID)).thenReturn(mis);
		assertEquals(mis, membershipInvitationManagerImpl.get(userInfo, MIS_ID));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testNonAdminDelete() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(mockMembershipInvtnSubmissionDAO.get(MIS_ID)).thenReturn(mis);
		membershipInvitationManagerImpl.delete(userInfo, MIS_ID);
	}
	
	@Test
	public void testAdminDelete() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockMembershipInvtnSubmissionDAO.get(MIS_ID)).thenReturn(mis);
		membershipInvitationManagerImpl.delete(userInfo, MIS_ID);
		Mockito.verify(mockMembershipInvtnSubmissionDAO).delete(MIS_ID);
	}
	

	@Test
	public void testGetOpenForUserInRange() throws Exception {
		MembershipInvitation mi = createMembershipInvitation();
		List<MembershipInvitation> expected = Arrays.asList(new MembershipInvitation[]{mi});
		when(mockMembershipInvtnSubmissionDAO.getOpenByUserInRange(eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipInvtnSubmissionDAO.getOpenByUserCount(eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipInvitation> actual = membershipInvitationManagerImpl.getOpenForUserInRange(MEMBER_PRINCIPAL_ID,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetOpenForUserAndTeamInRange() throws Exception {
		MembershipInvitation mi = createMembershipInvitation();
		List<MembershipInvitation> expected = Arrays.asList(new MembershipInvitation[]{mi});
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserInRange(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipInvitation> actual = membershipInvitationManagerImpl.getOpenForUserAndTeamInRange(MEMBER_PRINCIPAL_ID, TEAM_ID,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test
	public void testGetOpenSubmissionsForTeamInRange() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		List<MembershipInvtnSubmission> expected = Arrays.asList(new MembershipInvtnSubmission[]{mis});
		when(mockMembershipInvtnSubmissionDAO.getOpenSubmissionsByTeamInRange(eq(Long.parseLong(TEAM_ID)), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamCount(eq(Long.parseLong(TEAM_ID)), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipInvtnSubmission> actual = membershipInvitationManagerImpl.getOpenSubmissionsForTeamInRange(userInfo, TEAM_ID,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetOpenSubmissionsForTeamInRangeUnauthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		membershipInvitationManagerImpl.getOpenSubmissionsForTeamInRange(userInfo, TEAM_ID,1,0);
	}
	
	@Test
	public void testGetOpenSubmissionsForTeamAndRequesterInRange() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		List<MembershipInvtnSubmission> expected = Arrays.asList(new MembershipInvtnSubmission[]{mis});
		when(mockMembershipInvtnSubmissionDAO.getOpenSubmissionsByTeamAndUserInRange(eq(Long.parseLong(TEAM_ID)), anyLong(), anyLong(), anyLong(), anyLong())).
			thenReturn(expected);
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamCount(eq(Long.parseLong(TEAM_ID)), anyLong())).thenReturn((long)expected.size());
		PaginatedResults<MembershipInvtnSubmission> actual = membershipInvitationManagerImpl.
				getOpenSubmissionsForUserAndTeamInRange(userInfo, MEMBER_PRINCIPAL_ID, TEAM_ID,1,0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test(expected=UnauthorizedException.class)
	public void testGetOpenSubmissionsForTeamAndRequesterInRangeUnauthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		membershipInvitationManagerImpl.getOpenSubmissionsForUserAndTeamInRange(userInfo, MEMBER_PRINCIPAL_ID, TEAM_ID,1,0);
	}
	
	@Test
	public void testCreateInvitationNotification() throws Exception {
		MembershipInvtnSubmission mis = createMembershipInvtnSubmission(MIS_ID);
		Team team = new Team();
		team.setName("test team");
		team.setId(TEAM_ID);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		String acceptInvitationEndpoint = "https://synapse.org/#acceptInvitationEndpoint:";
		String notificationUnsubscribeEndpoint = "https://synapse.org/#notificationUnsubscribeEndpoint:";
		MessageToUserAndBody result = membershipInvitationManagerImpl.
				createInvitationNotification(mis, acceptInvitationEndpoint, notificationUnsubscribeEndpoint);
		assertEquals("You Have Been Invited to Join a Team", result.getMetadata().getSubject());
		assertEquals(Collections.singleton(MEMBER_PRINCIPAL_ID), result.getMetadata().getRecipients());
		assertEquals(notificationUnsubscribeEndpoint, result.getMetadata().getNotificationUnsubscribeEndpoint());
		
		// this will give us seven pieces...
		List<String> delims = Arrays.asList(new String[] {
				TEMPLATE_KEY_TEAM_NAME,
				TEMPLATE_KEY_INVITER_MESSAGE,
				TEMPLATE_KEY_ONE_CLICK_JOIN
		});
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate(MembershipInvitationManagerImpl.TEAM_MEMBERSHIP_INVITATION_EXTENDED_TEMPLATE, delims);

		assertTrue(result.getBody().startsWith(templatePieces.get(0)));
		assertTrue(result.getBody().indexOf(templatePieces.get(2))>0);
		String teamName = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(0), templatePieces.get(2));
		assertEquals("test team", teamName);
		assertTrue(result.getBody().indexOf(templatePieces.get(4))>0);
		String inviterMessage = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(2), templatePieces.get(4));
		assertTrue(inviterMessage.indexOf("Please join our team.")>=0);
		assertTrue(result.getBody().endsWith(templatePieces.get(6)));
		String acceptInvitationToken = 
				EmailParseUtil.getTokenFromString(result.getBody(), 
				templatePieces.get(4)+acceptInvitationEndpoint, templatePieces.get(6));
		JoinTeamSignedToken jtst = SerializationUtils.hexDecodeAndDeserialize(acceptInvitationToken, JoinTeamSignedToken.class);
		SignedTokenUtil.validateToken(jtst);
		assertEquals(TEAM_ID, jtst.getTeamId());
		assertEquals(MEMBER_PRINCIPAL_ID, jtst.getMemberId());
		assertEquals(MEMBER_PRINCIPAL_ID, jtst.getUserId());
	}
	

}
