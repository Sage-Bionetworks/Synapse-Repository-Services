package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_DISPLAY_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_REASON;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_USER_ID;
import static org.sagebionetworks.repo.manager.VerificationManagerImpl.VERIFICATION_NOTIFICATION_SUBJECT;
import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.APPROVED;
import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.REJECTED;
import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.SUBMITTED;
import static org.sagebionetworks.repo.model.verification.VerificationStateEnum.SUSPENDED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.team.EmailParseUtil;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.verification.AttachmentMetadata;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public class VerificationManagerImplTest {
	
	
	private static final Long USER_ID = 101L;
	private static final String USER_NAME = "username";
	private static final String FIRST_NAME = "fname";
	private static final String LAST_NAME = "lname";
	private static final String COMPANY = "company";
	private static final String LOCATION = "location";
	private static final String ORCID = "http://www.orcid.org/my-id";
	private static final List<String> EMAILS = Arrays.asList("primary.email.com", "secondary.email.com");
	private static final String FILE_HANDLE_ID = "101";
	private static final String FILE_NAME = "filename.txt";
	private static final String NOTIFICATION_UNSUBSCRIBE_ENDPOINT = "https://synapse.org/#notificationUnsubscribeEndpoint:";

	private static final Long VERIFICATION_ID = 222L;

	
	private VerificationDAO mockVerificationDao;
	
	private UserProfileManager mockUserProfileManager;
	
	private FileHandleManager mockFileHandleManager;
	
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	
	private AuthorizationManager mockAuthorizationManager;

	private TransactionalMessenger mockTransactionalMessenger;

	private VerificationManagerImpl verificationManager;
	
	private UserInfo userInfo;
	private VerificationSubmission verificationSubmission;
	

	private static UserProfile createUserProfile() {
		UserProfile userProfile = new UserProfile();
		userProfile.setUserName(USER_NAME);
		userProfile.setFirstName(FIRST_NAME);
		userProfile.setLastName(LAST_NAME);
		userProfile.setLocation(LOCATION);
		userProfile.setCompany(COMPANY);
		userProfile.setEmails(EMAILS);
		return userProfile;
	}
	
	private static VerificationSubmission createVerificationSubmission() {
		VerificationSubmission verificationSubmission = new VerificationSubmission();
		verificationSubmission.setFirstName(FIRST_NAME);
		verificationSubmission.setLastName(LAST_NAME);
		verificationSubmission.setLocation(LOCATION);
		verificationSubmission.setCompany(COMPANY);
		verificationSubmission.setEmails(EMAILS);
		AttachmentMetadata attachmentMetadata = new AttachmentMetadata();
		attachmentMetadata.setId(FILE_HANDLE_ID);
		verificationSubmission.setAttachments(Collections.singletonList(attachmentMetadata));
		verificationSubmission.setOrcid(ORCID);
		return verificationSubmission;
	}
	

	@Before
	public void setUp() throws Exception {
		userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);
		mockVerificationDao = Mockito.mock(VerificationDAO.class);
		mockUserProfileManager = Mockito.mock(UserProfileManager.class);
		mockFileHandleManager = Mockito.mock(FileHandleManager.class);
		mockPrincipalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockTransactionalMessenger = Mockito.mock(TransactionalMessenger.class);
		verificationManager = new VerificationManagerImpl(
				mockVerificationDao,
				mockUserProfileManager,
				mockFileHandleManager,
				mockPrincipalAliasDAO,
				mockAuthorizationManager,
				mockTransactionalMessenger);
		
		UserProfile userProfile = createUserProfile();
		when(mockUserProfileManager.getUserProfile(USER_ID.toString())).
			thenReturn(userProfile);
		PrincipalAlias orcidAlias = new PrincipalAlias();
		orcidAlias.setAlias(ORCID);
		List<PrincipalAlias> paList = Collections.singletonList(orcidAlias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_ORCID)).thenReturn(paList);
		PrincipalAlias actAlias = new PrincipalAlias();
		actAlias.setAlias("Synapse Access and Compliance Team");
		List<PrincipalAlias> actPaList = Collections.singletonList(actAlias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(
				TeamConstants.ACT_TEAM_ID, AliasType.TEAM_NAME)).
				thenReturn(actPaList);
		verificationSubmission = createVerificationSubmission();
		when(mockVerificationDao.
				createVerificationSubmission(verificationSubmission)).thenReturn(verificationSubmission);
		FileHandle fileHandle = Mockito.mock(FileHandle.class);
		when(fileHandle.getId()).thenReturn(FILE_HANDLE_ID);
		when(fileHandle.getFileName()).thenReturn(FILE_NAME);
		when(mockFileHandleManager.getRawFileHandle(userInfo, FILE_HANDLE_ID)).thenReturn(fileHandle);

	}
	
	@Test
	public void testCreateVerificationSubmission() {
		// method under test:
		verificationSubmission = verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
		
		verify(mockVerificationDao).getCurrentVerificationSubmissionForUser(USER_ID);
		verify(mockVerificationDao).createVerificationSubmission(verificationSubmission);
		assertEquals(USER_ID.toString(), verificationSubmission.getCreatedBy());
		assertNotNull(verificationSubmission.getCreatedOn());
		assertEquals(1, verificationSubmission.getAttachments().size());
		assertEquals(FILE_HANDLE_ID, verificationSubmission.getAttachments().get(0).getId());
		assertEquals(FILE_NAME, verificationSubmission.getAttachments().get(0).getFileName());
		verify(mockTransactionalMessenger).sendMessageAfterCommit(USER_ID.toString(), ObjectType.VERIFICATION_SUBMISSION, "etag", ChangeType.CREATE);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateVerificationSubmissionDuplicateAttachment() throws Exception {
		// Duplicate attachment
		AttachmentMetadata attachment = verificationSubmission.getAttachments().get(0);
		List<AttachmentMetadata> attachments = Arrays.asList(attachment, attachment);
		verificationSubmission.setAttachments(attachments);
		// Call under test
		verificationSubmission = verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
	}
	
	@Test
	public void testCreateVerificationSubmissionAlreadyCreated() {
		VerificationSubmission verificationSubmission = createVerificationSubmission();
		VerificationSubmission current = createVerificationSubmission();
		List<VerificationState> states = new ArrayList<VerificationState>();
		VerificationState state = new VerificationState();
		state.setState(VerificationStateEnum.SUBMITTED);
		states.add(state);
		current.setStateHistory(states);
		when(mockVerificationDao.getCurrentVerificationSubmissionForUser(USER_ID)).thenReturn(current);
		
		// can't create a submission when there's already a submitted one
		try {
			verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
			fail("exception expected");
		} catch (UnauthorizedException e) {
			// as expected
		}
		
		// can't create a submission when there's already an approved one
		state = new VerificationState();
		state.setState(VerificationStateEnum.APPROVED);
		states.add(state);
		try {
			verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
			fail("exception expected");
		} catch (UnauthorizedException e) {
			// as expected
		}

		// CAN crete a submission when there's a rejected one
		state.setState(VerificationStateEnum.REJECTED);
		verificationManager.
			createVerificationSubmission(userInfo, verificationSubmission);

		// CAN crete a submission when there's a suspended one
		state.setState(VerificationStateEnum.SUSPENDED);
		verificationManager.
			createVerificationSubmission(userInfo, verificationSubmission);
		verify(mockTransactionalMessenger, Mockito.times(2)).sendMessageAfterCommit(userInfo.getId().toString(), ObjectType.VERIFICATION_SUBMISSION, "etag", ChangeType.CREATE);
	}
	
	@Test
	public void testValidateVerificationSubmission() {
		UserProfile userProfile = createUserProfile();
		VerificationSubmission verificationSubmission = createVerificationSubmission();
		VerificationManagerImpl.validateVerificationSubmission(verificationSubmission, userProfile, ORCID);
		
		verificationSubmission = createVerificationSubmission();
		verificationSubmission.setFirstName("foo");
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		verificationSubmission.setFirstName(null);
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		
		verificationSubmission = createVerificationSubmission();
		verificationSubmission.setLastName("foo");
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		verificationSubmission.setLastName(null);
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		
		verificationSubmission = createVerificationSubmission();
		verificationSubmission.setLocation("foo");
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		verificationSubmission.setLocation(null);
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		
		verificationSubmission = createVerificationSubmission();
		verificationSubmission.setCompany("foo");
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		verificationSubmission.setCompany(null);
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		
		verificationSubmission = createVerificationSubmission();
		verificationSubmission.setEmails(Arrays.asList("foo"));
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		verificationSubmission.setEmails(null);
		checkInvalidSubmission(verificationSubmission, userProfile, ORCID);		
		
		// check wrong or missing ORCID
		checkInvalidSubmission(verificationSubmission, userProfile, "wrongorcid");
		checkInvalidSubmission(verificationSubmission, userProfile, null);
	}
	
	private static void checkInvalidSubmission(
			VerificationSubmission verificationSubmission, 
			UserProfile userProfile, 
			String orcId) {
		try {
			VerificationManagerImpl.validateVerificationSubmission(verificationSubmission, userProfile, orcId);
			fail("exception expected");
		} catch (RuntimeException e) {
			// as expected
		}
	}

	@Test(expected=UnauthorizedException.class)
	public void testCreateVerificationSubmissionUnauthorizedFile() {
		when(mockFileHandleManager.getRawFileHandle(userInfo, FILE_HANDLE_ID)).thenThrow(new UnauthorizedException());

		// method under test:
		verificationSubmission = verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
	}

	@Test
	public void testDeleteVerificationSubmission() {
		when(mockVerificationDao.getVerificationSubmitter(VERIFICATION_ID)).thenReturn(USER_ID);
		verificationManager.deleteVerificationSubmission(userInfo, VERIFICATION_ID);
		verify(mockVerificationDao).deleteVerificationSubmission(VERIFICATION_ID);
	}

	@Test(expected=UnauthorizedException.class)
	public void testDeleteVerificationSubmissionUnauthorized() {
		when(mockVerificationDao.getVerificationSubmitter(VERIFICATION_ID)).thenReturn(USER_ID*13);
		verificationManager.deleteVerificationSubmission(userInfo, VERIFICATION_ID);
	}

	@Test
	public void testListVerificationSubmissions() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		List<VerificationStateEnum> states = Collections.singletonList(VerificationStateEnum.SUBMITTED);
		VerificationSubmission vs = new VerificationSubmission();
		vs.setId("101");
		List<VerificationSubmission> vsList = Collections.singletonList(vs);
		when(mockVerificationDao.listVerificationSubmissions(
				states, USER_ID, 10, 0)).thenReturn(vsList);
		when(mockVerificationDao.countVerificationSubmissions(states, USER_ID)).thenReturn(1L);
		
		// method under test:
		VerificationPagedResults  result = 
				verificationManager.listVerificationSubmissions(userInfo, states, USER_ID, 10, 0);
		VerificationPagedResults expected = new VerificationPagedResults();
		expected.setResults(vsList);
		expected.setTotalNumberOfResults(1L);
		assertEquals(expected, result);
	}

	@Test(expected=UnauthorizedException.class)
	public void testListVerificationSubmissionsUnauthorized() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		List<VerificationStateEnum> states = Collections.singletonList(VerificationStateEnum.SUBMITTED);
		verificationManager.listVerificationSubmissions(userInfo, states, USER_ID, 10, 0);
	}

	@Test
	public void testChangeSubmissionState() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockVerificationDao.getVerificationState(VERIFICATION_ID)).thenReturn(
				VerificationStateEnum.SUBMITTED);
		
		VerificationState state = new VerificationState();
		state.setState(VerificationStateEnum.APPROVED);
		verificationManager.changeSubmissionState(userInfo, VERIFICATION_ID, state);

		assertEquals(USER_ID.toString(), state.getCreatedBy());
		assertNotNull(state.getCreatedOn());
		
		verify(mockVerificationDao).appendVerificationSubmissionState(VERIFICATION_ID, state);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(USER_ID.toString(), ObjectType.VERIFICATION_SUBMISSION, "etag", ChangeType.UPDATE);
	}

	@Test(expected=InvalidModelException.class)
	public void testChangeSubmissionStateTransitionNotAllowed() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockVerificationDao.getVerificationState(VERIFICATION_ID)).thenReturn(
				VerificationStateEnum.SUSPENDED);
		
		VerificationState state = new VerificationState();
		state.setState(VerificationStateEnum.APPROVED);
		verificationManager.changeSubmissionState(userInfo, VERIFICATION_ID, state);
	}
	
	public void testIsStateTransitionAllowed() {
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(SUBMITTED, SUBMITTED));
		assertTrue(VerificationManagerImpl.isStateTransitionAllowed(SUBMITTED, REJECTED));
		assertTrue(VerificationManagerImpl.isStateTransitionAllowed(SUBMITTED, APPROVED));
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(SUBMITTED, SUSPENDED));
		
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(REJECTED, SUBMITTED));
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(REJECTED, REJECTED));
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(REJECTED, APPROVED));
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(REJECTED, SUSPENDED));
		
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(APPROVED, SUBMITTED));
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(APPROVED, REJECTED));
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(APPROVED, APPROVED));
		assertTrue(VerificationManagerImpl.isStateTransitionAllowed(APPROVED, SUSPENDED));
		
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(SUSPENDED, SUBMITTED));
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(SUSPENDED, REJECTED));
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(SUSPENDED, APPROVED));
		assertFalse(VerificationManagerImpl.isStateTransitionAllowed(SUSPENDED, SUSPENDED));
		
	}


	@Test(expected=UnauthorizedException.class)
	public void testChangeSubmissionStateUnauthorzed() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		VerificationState state = new VerificationState();
		state.setState(VerificationStateEnum.APPROVED);
		verificationManager.changeSubmissionState(userInfo, VERIFICATION_ID, state);
	}


	@Test
	public void testCreateSubmissionNotification() throws Exception {
		VerificationSubmission verificationSubmission = createVerificationSubmission();
		verificationSubmission.setCreatedBy(USER_ID.toString());
		List<MessageToUserAndBody> mtubs = verificationManager.createSubmissionNotification(
				verificationSubmission, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		assertEquals(1, mtubs.size());
		MessageToUserAndBody result = mtubs.get(0);
		assertEquals(VERIFICATION_NOTIFICATION_SUBJECT, result.getMetadata().getSubject());
		assertEquals(Collections.singleton(TeamConstants.ACT_TEAM_ID.toString()), 
				result.getMetadata().getRecipients());
		assertEquals(NOTIFICATION_UNSUBSCRIBE_ENDPOINT, 
				result.getMetadata().getNotificationUnsubscribeEndpoint());
		assertEquals("text/html", result.getMimeType());
		assertEquals("Synapse Access and Compliance Team <synapseaccessandcomplianceteam@synapse.org>", result.getMetadata().getTo());
		
		List<String> delims = Arrays.asList(new String[] {
				TEMPLATE_KEY_DISPLAY_NAME,
				TEMPLATE_KEY_USER_ID
		});
		// this will create 5 pieces
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate(
				VerificationManagerImpl.VERIFICATION_SUBMISSION_TEMPLATE, delims);
		
		assertTrue(result.getBody().startsWith(templatePieces.get(0)));
		assertTrue(result.getBody().indexOf(templatePieces.get(2))>0);
		String displayName = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(0), templatePieces.get(2));
		assertEquals("fname lname (username)", displayName);	
		assertTrue(result.getBody().endsWith(templatePieces.get(4)));
		String userId = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(2), templatePieces.get(4));
		assertEquals(USER_ID.toString(), userId);
	}


	@Test
	public void testCreateStateChangeNotificationApproved() throws Exception {
		VerificationState newState = new VerificationState();
		newState.setState(APPROVED);
		when(mockVerificationDao.getVerificationSubmitter(VERIFICATION_ID)).thenReturn(USER_ID);

		// method under test
		List<MessageToUserAndBody> mtubs = verificationManager.createStateChangeNotification(
				VERIFICATION_ID, newState, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		assertEquals(1, mtubs.size());
		MessageToUserAndBody result = mtubs.get(0);
		assertEquals(VERIFICATION_NOTIFICATION_SUBJECT, result.getMetadata().getSubject());
		assertEquals(Collections.singleton(USER_ID.toString()), 
				result.getMetadata().getRecipients());
		assertEquals(NOTIFICATION_UNSUBSCRIBE_ENDPOINT, 
				result.getMetadata().getNotificationUnsubscribeEndpoint());
		assertEquals("text/html", result.getMimeType());
		
		System.out.println(result.getBody());
		
		List<String> delims = Arrays.asList(new String[] {
				TEMPLATE_KEY_DISPLAY_NAME,
				TEMPLATE_KEY_USER_ID
		});
		
		// this will create 5 pieces
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate(
				VerificationManagerImpl.VERIFICATION_APPROVED_TEMPLATE, delims);
		
		assertTrue(result.getBody().startsWith(templatePieces.get(0)));
		assertTrue(result.getBody().indexOf(templatePieces.get(2))>0);
		String displayName = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(0), templatePieces.get(2));
		assertEquals("fname lname (username)", displayName);	
		assertTrue(result.getBody().endsWith(templatePieces.get(4)));
		String userId = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(2), templatePieces.get(4));
		assertEquals(USER_ID.toString(), userId);
	}

	@Test
	public void testCreateStateChangeNotificationRejected() throws Exception {
		VerificationState newState = new VerificationState();
		newState.setState(REJECTED);
		String expectedReason = "your submission is invalid";
		newState.setReason(expectedReason);
		when(mockVerificationDao.getVerificationSubmitter(VERIFICATION_ID)).thenReturn(USER_ID);

		// method under test
		List<MessageToUserAndBody> mtubs = verificationManager.createStateChangeNotification(
				VERIFICATION_ID, newState, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		assertEquals(1, mtubs.size());
		MessageToUserAndBody result = mtubs.get(0);
		assertEquals(VERIFICATION_NOTIFICATION_SUBJECT, result.getMetadata().getSubject());
		assertEquals(Collections.singleton(USER_ID.toString()), 
				result.getMetadata().getRecipients());
		assertEquals(NOTIFICATION_UNSUBSCRIBE_ENDPOINT, 
				result.getMetadata().getNotificationUnsubscribeEndpoint());
		assertEquals("text/html", result.getMimeType());
		
		System.out.println(result.getBody());
		
		List<String> delims = Arrays.asList(new String[] {
				TEMPLATE_KEY_DISPLAY_NAME,
				TEMPLATE_KEY_REASON,
				TEMPLATE_KEY_USER_ID
		});
		
		// this will create 7 pieces
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate(
				VerificationManagerImpl.VERIFICATION_REJECTED_TEMPLATE, delims);
		
		assertTrue(result.getBody().startsWith(templatePieces.get(0)));
		assertTrue(result.getBody().indexOf(templatePieces.get(2))>0);
		String displayName = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(0), templatePieces.get(2));
		assertEquals("fname lname (username)", displayName);	
		assertTrue(result.getBody().indexOf(templatePieces.get(4))>0);
		String reason = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(2), templatePieces.get(4));
		assertEquals(expectedReason, reason);
		assertTrue(result.getBody().endsWith(templatePieces.get(6)));
		String userId = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(4), templatePieces.get(6));
		assertEquals(USER_ID.toString(), userId);
	}

	@Test
	public void testCreateStateChangeNotificationRejectedNoReason() throws Exception {
		VerificationState newState = new VerificationState();
		newState.setState(REJECTED);
		when(mockVerificationDao.getVerificationSubmitter(VERIFICATION_ID)).thenReturn(USER_ID);

		// method under test
		List<MessageToUserAndBody> mtubs = verificationManager.createStateChangeNotification(
				VERIFICATION_ID, newState, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		assertEquals(1, mtubs.size());
		MessageToUserAndBody result = mtubs.get(0);
		assertEquals(VERIFICATION_NOTIFICATION_SUBJECT, result.getMetadata().getSubject());
		assertEquals(Collections.singleton(USER_ID.toString()), 
				result.getMetadata().getRecipients());
		assertEquals(NOTIFICATION_UNSUBSCRIBE_ENDPOINT, 
				result.getMetadata().getNotificationUnsubscribeEndpoint());
		assertEquals("text/html", result.getMimeType());
		
		System.out.println(result.getBody());
		
		List<String> delims = Arrays.asList(new String[] {
				TEMPLATE_KEY_DISPLAY_NAME,
				TEMPLATE_KEY_USER_ID
		});
		
		// this will create 5 pieces
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate(
				VerificationManagerImpl.VERIFICATION_REJECTED_NO_REASON_TEMPLATE, delims);
		
		assertTrue(result.getBody().startsWith(templatePieces.get(0)));
		assertTrue(result.getBody().indexOf(templatePieces.get(2))>0);
		String displayName = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(0), templatePieces.get(2));
		assertEquals("fname lname (username)", displayName);	
		assertTrue(result.getBody().endsWith(templatePieces.get(4)));
		String userId = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(2), templatePieces.get(4));
		assertEquals(USER_ID.toString(), userId);
	}

	@Test
	public void testCreateStateChangeNotificationSuspended() throws Exception {
		VerificationState newState = new VerificationState();
		newState.setState(SUSPENDED);
		String expectedReason = "your submission is invalid";
		newState.setReason(expectedReason);
		when(mockVerificationDao.getVerificationSubmitter(VERIFICATION_ID)).thenReturn(USER_ID);

		// method under test
		List<MessageToUserAndBody> mtubs = verificationManager.createStateChangeNotification(
				VERIFICATION_ID, newState, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		assertEquals(1, mtubs.size());
		MessageToUserAndBody result = mtubs.get(0);
		assertEquals(VERIFICATION_NOTIFICATION_SUBJECT, result.getMetadata().getSubject());
		assertEquals(Collections.singleton(USER_ID.toString()), 
				result.getMetadata().getRecipients());
		assertEquals(NOTIFICATION_UNSUBSCRIBE_ENDPOINT, 
				result.getMetadata().getNotificationUnsubscribeEndpoint());
		assertEquals("text/html", result.getMimeType());
		
		System.out.println(result.getBody());
		
		List<String> delims = Arrays.asList(new String[] {
				TEMPLATE_KEY_DISPLAY_NAME,
				TEMPLATE_KEY_REASON,
				TEMPLATE_KEY_USER_ID
		});
		
		// this will create 7 pieces
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate(
				VerificationManagerImpl.VERIFICATION_SUSPENDED_TEMPLATE, delims);
		
		assertTrue(result.getBody().startsWith(templatePieces.get(0)));
		assertTrue(result.getBody().indexOf(templatePieces.get(2))>0);
		String displayName = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(0), templatePieces.get(2));
		assertEquals("fname lname (username)", displayName);	
		assertTrue(result.getBody().indexOf(templatePieces.get(4))>0);
		String reason = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(2), templatePieces.get(4));
		assertEquals(expectedReason, reason);
		assertTrue(result.getBody().endsWith(templatePieces.get(6)));
		String userId = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(4), templatePieces.get(6));
		assertEquals(USER_ID.toString(), userId);
	}

	@Test
	public void testCreateStateChangeNotificationSuspendedNoReason() throws Exception {
		VerificationState newState = new VerificationState();
		newState.setState(SUSPENDED);
		when(mockVerificationDao.getVerificationSubmitter(VERIFICATION_ID)).thenReturn(USER_ID);

		// method under test
		List<MessageToUserAndBody> mtubs = verificationManager.createStateChangeNotification(
				VERIFICATION_ID, newState, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		assertEquals(1, mtubs.size());
		MessageToUserAndBody result = mtubs.get(0);
		assertEquals(VERIFICATION_NOTIFICATION_SUBJECT, result.getMetadata().getSubject());
		assertEquals(Collections.singleton(USER_ID.toString()), 
				result.getMetadata().getRecipients());
		assertEquals(NOTIFICATION_UNSUBSCRIBE_ENDPOINT, 
				result.getMetadata().getNotificationUnsubscribeEndpoint());
		assertEquals("text/html", result.getMimeType());
		
		System.out.println(result.getBody());
		
		List<String> delims = Arrays.asList(new String[] {
				TEMPLATE_KEY_DISPLAY_NAME,
				TEMPLATE_KEY_USER_ID
		});
		
		// this will create 5 pieces
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate(
				VerificationManagerImpl.VERIFICATION_SUSPENDED_NO_REASON_TEMPLATE, delims);
		
		assertTrue(result.getBody().startsWith(templatePieces.get(0)));
		assertTrue(result.getBody().indexOf(templatePieces.get(2))>0);
		String displayName = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(0), templatePieces.get(2));
		assertEquals("fname lname (username)", displayName);	
		assertTrue(result.getBody().endsWith(templatePieces.get(4)));
		String userId = EmailParseUtil.getTokenFromString(result.getBody(), templatePieces.get(2), templatePieces.get(4));
		assertEquals(USER_ID.toString(), userId);
	}


}
