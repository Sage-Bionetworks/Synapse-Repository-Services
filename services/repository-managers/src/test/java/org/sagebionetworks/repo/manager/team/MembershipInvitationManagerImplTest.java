package org.sagebionetworks.repo.manager.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.team.MembershipInvitationManagerImpl.TWENTY_FOUR_HOURS_IN_MS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvitationDAO;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

@ExtendWith(MockitoExtension.class)
public class MembershipInvitationManagerImplTest {

	private static final String MEMBER_PRINCIPAL_ID = "999";
	private static final String INVITEE_EMAIL = "invitee@test.com";

	private static final String TEAM_ID = "123";
	private static final String MIS_ID = "987";

	private static MembershipInvitation createMembershipInvtnSubmission(String id) {
		MembershipInvitation mis = new MembershipInvitation();
		mis.setId(id);
		mis.setTeamId(TEAM_ID);
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		mis.setMessage("Please join our team.");
		return mis;
	}

	private static MembershipInvitation createMembershipInvtnSubmissionToEmail(String id) {
		MembershipInvitation mis = new MembershipInvitation();
		mis.setId(id);
		mis.setTeamId(TEAM_ID);
		mis.setInviteeEmail(INVITEE_EMAIL);
		Date now = new Date();
		mis.setCreatedOn(now);
		mis.setExpiresOn(new Date(now.getTime() + TWENTY_FOUR_HOURS_IN_MS));
		mis.setMessage("Please join our team.");
		return mis;
	}

	private static MembershipInvitation createMembershipInvitation() {
		MembershipInvitation mis = new MembershipInvitation();
		mis.setTeamId(TEAM_ID);
		mis.setInviteeId(MEMBER_PRINCIPAL_ID);
		return mis;
	}

	@Mock
	private AuthorizationManager mockAuthorizationManager = null;
	@Mock
	private MembershipInvitationDAO mockMembershipInvitationDAO = null;
	@Mock
	private TeamDAO mockTeamDAO = null;
	@Mock
	private SynapseEmailService mockSynapseEmailService;
	@Mock
	private TokenGenerator mockTokenGenerator;
	@Mock
	private EmailQuarantineDao mockEmailQuarantineDao;
	@Mock
	private MessageManager mockMessageManager;
	@Mock
	private UserProfileManager mockUserProfileManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDao;
	
	@InjectMocks
	private MembershipInvitationManagerImpl membershipInvitationManagerImpl;
	
	private UserInfo userInfo = null;
	private UserProfile userProfile = null;

	@BeforeEach
	public void setUp() throws Exception {
		userInfo = new UserInfo(false, MEMBER_PRINCIPAL_ID);
		userInfo.setGroups(Collections.singleton(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		userProfile = new UserProfile();
		userProfile.setFirstName("First");
		userProfile.setLastName("Last");
		userProfile.setUserName("username");
	}

	private void validateForCreateExpectFailure(MembershipInvitation mis) {
		Assertions.assertThrows(InvalidModelException.class, ()-> {
			MembershipInvitationManagerImpl.validateForCreate(mis);
		});
	}

	@Test
	public void testValidateForCreate() throws Exception {
		MembershipInvitation mis = new MembershipInvitation();

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
		MembershipInvitation mis = new MembershipInvitation();
		Date now = new Date();
		MembershipInvitationManagerImpl.populateCreationFields(userInfo, mis, now);
		assertEquals(MEMBER_PRINCIPAL_ID, mis.getCreatedBy());
		assertEquals(now, mis.getCreatedOn());
	}

	@Test
	public void testNonAdminCreate() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(null);
		
		when(mockAuthorizationManager.canAccessMembershipInvitation(userInfo, mis, ACCESS_TYPE.CREATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipInvitationManagerImpl.create(userInfo, mis);
		});
	}

	@Test
	public void testAdminCreate() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(null);
		when(mockAuthorizationManager.canAccessMembershipInvitation(userInfo, mis, ACCESS_TYPE.CREATE))
				.thenReturn(AuthorizationStatus.authorized());
		membershipInvitationManagerImpl.create(userInfo, mis);
		Mockito.verify(mockMembershipInvitationDAO).create(mis);
	}

	@Test
	public void testNonAdminGet() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccessMembershipInvitation(userInfo, mis, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockMembershipInvitationDAO.get(MIS_ID)).thenReturn(mis);
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipInvitationManagerImpl.get(userInfo, MIS_ID);
		});
	}

	@Test
	public void testAdminGet() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccessMembershipInvitation(userInfo, mis, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockMembershipInvitationDAO.get(MIS_ID)).thenReturn(mis);
		assertEquals(mis, membershipInvitationManagerImpl.get(userInfo, MIS_ID));
	}

	@Test
	public void testGetWithMembershipInvtnSignedToken() {
		MembershipInvitation mis = createMembershipInvtnSubmissionToEmail(MIS_ID);
		MembershipInvtnSignedToken token = new MembershipInvtnSignedToken();
		when(mockAuthorizationManager.canAccessMembershipInvitation(any(MembershipInvtnSignedToken.class),
				eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());
		when(mockMembershipInvitationDAO.get(MIS_ID)).thenReturn(mis);
		assertEquals(mis, membershipInvitationManagerImpl.get(MIS_ID, token));
	}

	@Test
	public void testNonAdminDelete() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccessMembershipInvitation(userInfo, mis, ACCESS_TYPE.DELETE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockMembershipInvitationDAO.get(MIS_ID)).thenReturn(mis);
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipInvitationManagerImpl.delete(userInfo, MIS_ID);
		});
	}

	@Test
	public void testAdminDelete() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccessMembershipInvitation(userInfo, mis, ACCESS_TYPE.DELETE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockMembershipInvitationDAO.get(MIS_ID)).thenReturn(mis);
		membershipInvitationManagerImpl.delete(userInfo, MIS_ID);
		Mockito.verify(mockMembershipInvitationDAO).delete(MIS_ID);
	}

	@Test
	public void testGetOpenForUserInRange() throws Exception {
		MembershipInvitation mis = createMembershipInvitation();
		List<MembershipInvitation> expected = Arrays.asList(new MembershipInvitation[] { mis });
		when(mockMembershipInvitationDAO.getOpenByUserInRange(eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong(),
				anyLong(), anyLong())).thenReturn(expected);
		when(mockMembershipInvitationDAO.getOpenByUserCount(eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong()))
				.thenReturn((long) expected.size());
		PaginatedResults<MembershipInvitation> actual = membershipInvitationManagerImpl
				.getOpenForUserInRange(MEMBER_PRINCIPAL_ID, 1, 0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test
	public void testGetOpenForUserAndTeamInRange() throws Exception {
		MembershipInvitation mis = createMembershipInvitation();
		List<MembershipInvitation> expected = Arrays.asList(new MembershipInvitation[] { mis });
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserInRange(eq(Long.parseLong(TEAM_ID)),
				eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong(), anyLong(), anyLong())).thenReturn(expected);
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)),
				eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn((long) expected.size());
		PaginatedResults<MembershipInvitation> actual = membershipInvitationManagerImpl
				.getOpenForUserAndTeamInRange(MEMBER_PRINCIPAL_ID, TEAM_ID, 1, 0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test
	public void testGetOpenSubmissionsForTeamInRange() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM,
				ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		List<MembershipInvitation> expected = Arrays.asList(new MembershipInvitation[] { mis });
		when(mockMembershipInvitationDAO.getOpenByTeamInRange(eq(Long.parseLong(TEAM_ID)), anyLong(), anyLong(),
				anyLong())).thenReturn(expected);
		when(mockMembershipInvitationDAO.getOpenByTeamCount(eq(Long.parseLong(TEAM_ID)), anyLong()))
				.thenReturn((long) expected.size());
		PaginatedResults<MembershipInvitation> actual = membershipInvitationManagerImpl
				.getOpenSubmissionsForTeamInRange(userInfo, TEAM_ID, 1, 0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test
	public void testGetOpenSubmissionsForTeamInRangeUnauthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipInvitationManagerImpl.getOpenSubmissionsForTeamInRange(userInfo, TEAM_ID, 1, 0);
		});
	}

	@Test
	public void testGetOpenSubmissionsForTeamAndRequesterInRange() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(MIS_ID);
		when(mockAuthorizationManager.canAccess(userInfo, mis.getTeamId(), ObjectType.TEAM,
				ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		List<MembershipInvitation> expected = Arrays.asList(new MembershipInvitation[] { mis });
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserInRange(eq(Long.parseLong(TEAM_ID)), anyLong(), anyLong(),
				anyLong(), anyLong())).thenReturn(expected);
		when(mockMembershipInvitationDAO.getOpenByTeamCount(eq(Long.parseLong(TEAM_ID)), anyLong()))
				.thenReturn((long) expected.size());
		PaginatedResults<MembershipInvitation> actual = membershipInvitationManagerImpl
				.getOpenSubmissionsForUserAndTeamInRange(userInfo, MEMBER_PRINCIPAL_ID, TEAM_ID, 1, 0);
		assertEquals(expected, actual.getResults());
		assertEquals(1L, actual.getTotalNumberOfResults());
	}

	@Test
	public void testGetOpenSubmissionsForTeamAndRequesterInRangeUnauthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			membershipInvitationManagerImpl.getOpenSubmissionsForUserAndTeamInRange(userInfo, MEMBER_PRINCIPAL_ID, TEAM_ID,
					1, 0);
		});
	}
	
	@Test
	public void testBuildSenderDisplayName() {
		String alternativeName = userProfile.getUserName();
		
		when(mockUserProfileManager.getUserProfile(anyString())).thenReturn(userProfile);
		
		// Call under test
		String result = membershipInvitationManagerImpl.buildSenderDisplayName(userInfo, alternativeName);
		
		assertEquals("First Last", result);
		
		verify(mockUserProfileManager).getUserProfile(userInfo.getId().toString());
	}
	
	@Test
	public void testBuildSenderDisplayNameWithUserNameOnly() {
		String alternativeName = "something else";
		
		userProfile.setFirstName(null).setLastName(null);
		
		when(mockUserProfileManager.getUserProfile(anyString())).thenReturn(userProfile);
		
		// Call under test
		String result = membershipInvitationManagerImpl.buildSenderDisplayName(userInfo, alternativeName);
		
		assertEquals(userProfile.getUserName(), result);
		
		verify(mockUserProfileManager).getUserProfile(userInfo.getId().toString());
	}
	
	@Test
	public void testBuildSenderDisplayNameWithNoProfile() {
		String alternativeName = userProfile.getUserName();
		
		doThrow(NotFoundException.class).when(mockUserProfileManager).getUserProfile(anyString());
		
		// Call under test
		String result = membershipInvitationManagerImpl.buildSenderDisplayName(userInfo, alternativeName);
		
		assertEquals(alternativeName, result);
		
		verify(mockUserProfileManager).getUserProfile(userInfo.getId().toString());
	}
	
	@Test
	public void testBuildInvitationEmailSubject() {
		assertEquals("First Last has invited you to join the Awesome team", membershipInvitationManagerImpl.buildInvitationEmailSubject("First Last", "Awesome"));
	}
	
	@Test
	public void testBuildInvitationEmailBody() {
		String sender = "First Last";
		String teamName = "Team";
		String oneClickJoinLink = "http://some-awesome-url.org";
		String senderMessage = "Some awesome message";
		
		String result = membershipInvitationManagerImpl.buildInvitationEmailBody(sender, teamName, oneClickJoinLink, senderMessage);
		
		assertTrue(result.contains("First Last is inviting you to join the Team team."));
		assertTrue(result.contains("The inviter sends the following message: <Blockquote> Some awesome message </Blockquote> "));
		assertTrue(result.contains("href=\"http://some-awesome-url.org\""));
	}
	
	@Test
	public void testBuildInvitationEmailBodyWithEmptyMessage() {
		String sender = "First Last";
		String teamName = "Team";
		String oneClickJoinLink = "http://some-awesome-url.org";
		String senderMessage = " ";
		
		String result = membershipInvitationManagerImpl.buildInvitationEmailBody(sender, teamName, oneClickJoinLink, senderMessage);
		
		assertTrue(result.contains("First Last is inviting you to join the Team team."));
		assertFalse(result.contains("The inviter sends the following message"));
		assertTrue(result.contains("href=\"http://some-awesome-url.org\""));
	}
	
	@Test
	public void testBuildInvitationEmailBodyWithNullMessage() {
		String sender = "First Last";
		String teamName = "Team";
		String oneClickJoinLink = "http://some-awesome-url.org";
		String senderMessage = null;
		
		String result = membershipInvitationManagerImpl.buildInvitationEmailBody(sender, teamName, oneClickJoinLink, senderMessage);
		
		assertTrue(result.contains("First Last is inviting you to join the Team team."));
		assertFalse(result.contains("The inviter sends the following message"));
		assertTrue(result.contains("href=\"http://some-awesome-url.org\""));
	}

	@Test
	public void testSendInvitationEmailToSynapseUser() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(MIS_ID);
		testSendInvitationEmailToSynapseUserHelper(mis, "First Last");
	}
	
	@Test
	public void testSendInvitationEmailToSynapseUserWithUserName() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(MIS_ID);
		userProfile.setFirstName(null).setLastName(null);
		testSendInvitationEmailToSynapseUserHelper(mis, "username");
	}

	@Test
	public void testSendInvitationEmailToSynapseUserWithNullCreatedOn() throws Exception {
		MembershipInvitation mis = createMembershipInvtnSubmission(MIS_ID);
		mis.setCreatedOn(null);
		testSendInvitationEmailToSynapseUserHelper(mis, "First Last");
	}

	private void testSendInvitationEmailToSynapseUserHelper(MembershipInvitation mis, String expectedSender) throws UnsupportedEncodingException, IOException {
		Team team = new Team();
		team.setName("test team");
		team.setId(TEAM_ID);
		
		String fileHandleId = "123";
		
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		when(mockPrincipalAliasDao.getUserName(anyLong())).thenReturn(userProfile.getUserName());
		when(mockUserProfileManager.getUserProfile(anyString())).thenReturn(userProfile);
		when(mockFileHandleManager.createCompressedFileFromString(any(), any(), any(), any())).thenReturn(new S3FileHandle().setId(fileHandleId));
		
		
		String acceptInvitationEndpoint = "https://synapse.org/#acceptInvitationEndpoint:";
		String notificationUnsubscribeEndpoint = "https://synapse.org/#notificationUnsubscribeEndpoint:";
		
		// Call under test
		membershipInvitationManagerImpl.sendInvitationEmailToSynapseUser(userInfo, mis, acceptInvitationEndpoint, notificationUnsubscribeEndpoint);
		
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		
		verify(mockFileHandleManager).createCompressedFileFromString(eq(MEMBER_PRINCIPAL_ID), any(), argument.capture(), eq(ContentType.TEXT_HTML.getMimeType()));
		
		String body = argument.getValue();
		
		assertTrue(body.contains(team.getName()));
		assertTrue(body.contains(mis.getMessage()));
		assertTrue(body.contains(acceptInvitationEndpoint));
		
		MessageToUser expectedMessage = new MessageToUser();
		expectedMessage.setFileHandleId(fileHandleId);
		expectedMessage.setRecipients(Collections.singleton(MEMBER_PRINCIPAL_ID));
		expectedMessage.setSubject(expectedSender + " has invited you to join the test team team");
		expectedMessage.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
		expectedMessage.setWithProfileSettingLink(false);
		expectedMessage.setWithUnsubscribeLink(true);
		expectedMessage.setIsNotificationMessage(false);
		
		verify(mockMessageManager).createMessage(userInfo, expectedMessage);
		
	}

	@Test
	public void testSendInvitationEmailToEmail() throws Exception {
		Team team = new Team();
		String teamName = "Test team";
		team.setName(teamName);
		team.setId(TEAM_ID);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		when(mockPrincipalAliasDao.getUserName(anyLong())).thenReturn(userProfile.getUserName());
		when(mockUserProfileManager.getUserProfile(anyString())).thenReturn(userProfile);
		
		MembershipInvitation mis = createMembershipInvtnSubmissionToEmail(MIS_ID);
		String acceptInvitationEndpoint = "https://synapse.org/#acceptInvitationEndpoint:";
		
		// Call under test
		membershipInvitationManagerImpl.sendInvitationEmailToEmail(userInfo, mis, acceptInvitationEndpoint);
		
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		Mockito.verify(mockSynapseEmailService).sendRawEmail(argument.capture());
		SendRawEmailRequest emailRequest = argument.getValue();
		assertEquals(Collections.singletonList(INVITEE_EMAIL), emailRequest.getDestinations());
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(emailRequest.getRawMessage().getData().array()));
		String body = (String) ((MimeMultipart) mimeMessage.getContent()).getBodyPart(0).getContent();
		assertNotNull(mimeMessage.getSubject());
		assertFalse(body.contains(mis.getTeamId())); //PLFM-5369: Users kept clicking the team page instead of joining the team via invitation link.
		assertTrue(body.contains(teamName));
		assertTrue(body.contains(mis.getMessage()));
		assertTrue(body.contains(acceptInvitationEndpoint));
		assertEquals("First Last has invited you to join the Test team team", mimeMessage.getSubject());
		assertEquals("First Last <username@synapse.org>", emailRequest.getSource());
	}
	
	@Test
	public void testSendInvitationEmailToEmailWithUsernameOnly() throws Exception {
		Team team = new Team();
		String teamName = "Test team";
		team.setName(teamName);
		team.setId(TEAM_ID);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		when(mockPrincipalAliasDao.getUserName(anyLong())).thenReturn(userProfile.getUserName());
		when(mockUserProfileManager.getUserProfile(anyString())).thenReturn(userProfile);
		
		userProfile.setFirstName(null).setLastName(null);
		
		MembershipInvitation mis = createMembershipInvtnSubmissionToEmail(MIS_ID);
		String acceptInvitationEndpoint = "https://synapse.org/#acceptInvitationEndpoint:";
		
		// Call under test
		membershipInvitationManagerImpl.sendInvitationEmailToEmail(userInfo, mis, acceptInvitationEndpoint);
		
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		Mockito.verify(mockSynapseEmailService).sendRawEmail(argument.capture());
		SendRawEmailRequest emailRequest = argument.getValue();
		assertEquals(Collections.singletonList(INVITEE_EMAIL), emailRequest.getDestinations());
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(emailRequest.getRawMessage().getData().array()));
		String body = (String) ((MimeMultipart) mimeMessage.getContent()).getBodyPart(0).getContent();
		assertNotNull(mimeMessage.getSubject());
		assertFalse(body.contains(mis.getTeamId())); //PLFM-5369: Users kept clicking the team page instead of joining the team via invitation link.
		assertTrue(body.contains(teamName));
		assertTrue(body.contains(mis.getMessage()));
		assertTrue(body.contains(acceptInvitationEndpoint));
		assertEquals("username has invited you to join the Test team team", mimeMessage.getSubject());
		assertEquals("username@synapse.org", emailRequest.getSource());
	}
	
	@Test
	public void testSendInvitationEmailToEmailNotCertified() throws Exception {
		// Remove the certified group
		userInfo.setGroups(Collections.emptySet());
		
		MembershipInvitation mis = createMembershipInvtnSubmissionToEmail(MIS_ID);
		String acceptInvitationEndpoint = "https://synapse.org/#acceptInvitationEndpoint:";
		
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			membershipInvitationManagerImpl.sendInvitationEmailToEmail(userInfo, mis, acceptInvitationEndpoint);
		});
		
		assertEquals("You must be a certified user to send email invitations", result.getMessage());
	}
	
	@Test
	public void testSendInvitationEmailToEmailWithQuarantinedAddress() throws Exception {
		
		when(mockEmailQuarantineDao.isQuarantined(INVITEE_EMAIL)).thenReturn(true);
		MembershipInvitation mis = createMembershipInvtnSubmissionToEmail(MIS_ID);
		
		Assertions.assertThrows(QuarantinedEmailException.class, ()-> {
			// Call under test
			membershipInvitationManagerImpl.sendInvitationEmailToEmail(userInfo, mis, null);
		});
		
	}

	@Test
	public void testGetOpenInvitationCountForUserWithNullPrincipalId() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			membershipInvitationManagerImpl.getOpenInvitationCountForUser(null);
		});
	}

	@Test
	public void testGetOpenInvitationCountForUser() {
		Long count = 7L;
		when(mockMembershipInvitationDAO.getOpenByUserCount(anyLong(), anyLong())).thenReturn(count);
		Count result = membershipInvitationManagerImpl.getOpenInvitationCountForUser(MEMBER_PRINCIPAL_ID);
		assertNotNull(result);
		assertEquals(count, result.getCount());
	}

	@Test
	public void testVerifyInvitee() {
		// Setup
		MembershipInvitation mis = createMembershipInvtnSubmissionToEmail(MIS_ID);
		when(mockMembershipInvitationDAO.get(MIS_ID)).thenReturn(mis);
		Long userId = Long.parseLong(MEMBER_PRINCIPAL_ID);
		// Mock listPrincipalAliases to return one alias with the invitee email
		PrincipalAliasDAO mockPrincipalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		ReflectionTestUtils.setField(membershipInvitationManagerImpl, "principalAliasDAO", mockPrincipalAliasDAO);
		when(mockPrincipalAliasDAO.aliasIsBoundToPrincipal(INVITEE_EMAIL, MEMBER_PRINCIPAL_ID)).thenReturn(true);

		// Test getInviteeVerificationSignedToken by inspecting the token it returns
		InviteeVerificationSignedToken token = membershipInvitationManagerImpl.getInviteeVerificationSignedToken(userId,
				MIS_ID);
		assertNotNull(token);
		assertEquals(MEMBER_PRINCIPAL_ID, token.getInviteeId());
		assertEquals(MIS_ID, token.getMembershipInvitationId());
		mockTokenGenerator.validateToken(token);

		// Test failure cases
		// Failure 1 - mis is expired
		Date expiresOn = mis.getExpiresOn();
		mis.setExpiresOn(new Date(new Date().getTime() - 999999L));
		boolean caughtException = false;
		try {
			membershipInvitationManagerImpl.getInviteeVerificationSignedToken(userId, MIS_ID);
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		assertTrue(caughtException);
		// Restore expiresOn
		mis.setExpiresOn(expiresOn);

		// Failure 2 - inviteeId is set
		mis.setInviteeId("not-null");
		caughtException = false;
		try {
			membershipInvitationManagerImpl.getInviteeVerificationSignedToken(userId, MIS_ID);
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		assertTrue(caughtException);
		mis.setInviteeId(null);

		// Failure 3 - invitee email is not associated with user
		when(mockPrincipalAliasDAO.aliasIsBoundToPrincipal(INVITEE_EMAIL, MEMBER_PRINCIPAL_ID)).thenReturn(false);
		caughtException = false;
		try {
			membershipInvitationManagerImpl.getInviteeVerificationSignedToken(userId, MIS_ID);
		} catch (UnauthorizedException e) {
			caughtException = true;
		}
		assertTrue(caughtException);
	}

	@Test
	public void testUpdateId() {
		// Setup
		Long userId = Long.parseLong(MEMBER_PRINCIPAL_ID);
		MembershipInvitation mis = createMembershipInvtnSubmissionToEmail(MIS_ID);
		InviteeVerificationSignedToken token = new InviteeVerificationSignedToken();
		token.setMembershipInvitationId(MIS_ID);
		token.setExpiresOn(mis.getExpiresOn());
		// Mock happy case behavior
		when(mockAuthorizationManager.canAccessMembershipInvitation(userId, token, ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockMembershipInvitationDAO.getWithUpdateLock(MIS_ID)).thenReturn(mis);

		// Happy case should succeed
		membershipInvitationManagerImpl.updateInviteeId(userId, MIS_ID, token);
		Mockito.verify(mockMembershipInvitationDAO).updateInviteeId(MIS_ID, userId);

		// URI id and signed token id should match
		boolean caughtException = false;
		try {
			membershipInvitationManagerImpl.updateInviteeId(userId, "incorrectId", token);
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		assertTrue(caughtException);

		// Token with null expiresOn should fail
		token.setExpiresOn(null);
		caughtException = false;
		try {
			membershipInvitationManagerImpl.updateInviteeId(userId, MIS_ID, token);
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		assertTrue(caughtException);
		// Restore valid expiration date
		token.setExpiresOn(mis.getExpiresOn());

		// Expired token should fail
		token.setExpiresOn(new Date(new Date().getTime() - TWENTY_FOUR_HOURS_IN_MS));
		caughtException = false;
		try {
			membershipInvitationManagerImpl.updateInviteeId(userId, MIS_ID, token);
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		assertTrue(caughtException);
		// Restore valid expiration date
		token.setExpiresOn(mis.getExpiresOn());

		// Mock the authorization manager so that it denies access
		when(mockAuthorizationManager.canAccessMembershipInvitation(userId, token, ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		// Updating the inviteeId should throw an UnauthorizedException
		caughtException = false;
		try {
			membershipInvitationManagerImpl.updateInviteeId(userId, MIS_ID, token);
		} catch (UnauthorizedException e) {
			caughtException = true;
		}
		assertTrue(caughtException);

		// Restore the authorization manager to allow access again
		when(mockAuthorizationManager.canAccessMembershipInvitation(userId, token, ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		// Set the existing invitation's inviteeId
		mis.setInviteeId(userId.toString());
		// Updating the inviteeId should throw an UnauthorizedException
		caughtException = false;
		try {
			membershipInvitationManagerImpl.updateInviteeId(userId, MIS_ID, token);
		} catch (IllegalArgumentException e) {
			caughtException = true;
		}
		assertTrue(caughtException);
	}
}
