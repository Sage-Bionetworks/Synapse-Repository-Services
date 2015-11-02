package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public class VerificationManagerImplTest {
	
	private VerificationDAO mockVerificationDao;
	
	private UserProfileManager mockUserProfileManager;
	
	private FileHandleManager mockFileHandleManager;
	
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	
	private AuthorizationManager mockAuthorizationManager;

	private VerificationManagerImpl verificationManager;
	
	private static final Long USER_ID = 101L;
	private UserInfo userInfo;

	@Before
	public void setUp() throws Exception {
		userInfo = new UserInfo(false);
		userInfo.setId(USER_ID);
		mockVerificationDao = Mockito.mock(VerificationDAO.class);
		mockUserProfileManager = Mockito.mock(UserProfileManager.class);
		mockFileHandleManager = Mockito.mock(FileHandleManager.class);
		mockPrincipalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		verificationManager = new VerificationManagerImpl(
				mockVerificationDao,
				mockUserProfileManager,
				mockFileHandleManager,
				mockPrincipalAliasDAO,
				mockAuthorizationManager);
	}
	
	private static final String FIRST_NAME = "fname";
	private static final String LAST_NAME = "lname";
	private static final String COMPANY = "company";
	private static final String LOCATION = "location";
	private static final String ORCID = "http://www.orcid.org/my-id";
	private static final List<String> EMAILS = Arrays.asList("primary.email.com", "secondary.email.com");
	private static final List<String> FILES = Arrays.asList("888", "999");
	
	
	private static UserProfile createUserProfile() {
		UserProfile userProfile = new UserProfile();
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
		verificationSubmission.setFiles(FILES);
		verificationSubmission.setOrcid(ORCID);
		return verificationSubmission;
	}
	

	@Test
	public void testCreateVerificationSubmission() {
		UserProfile userProfile = createUserProfile();
		when(mockUserProfileManager.getUserProfile(USER_ID.toString())).
			thenReturn(userProfile);
		PrincipalAlias orcidAlias = new PrincipalAlias();
		orcidAlias.setAlias(ORCID);
		List<PrincipalAlias> paList = Collections.singletonList(orcidAlias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_ORCID)).thenReturn(paList);
		when(mockAuthorizationManager.canAccessRawFileHandleById(eq(userInfo), anyString())).
			thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		VerificationSubmission verificationSubmission = createVerificationSubmission();
		when(mockVerificationDao.
				createVerificationSubmission(verificationSubmission)).thenReturn(verificationSubmission);
				
		// method under test:
		verificationSubmission = verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
		
		verify(mockVerificationDao).getCurrentVerificationSubmissionForUser(USER_ID);
		verify(mockVerificationDao).createVerificationSubmission(verificationSubmission);
		assertEquals(USER_ID.toString(), verificationSubmission.getCreatedBy());
		assertNotNull(verificationSubmission.getCreatedOn());
	}
	
	@Test
	public void testCreateVerificationSubmissionAlreadyCreated() {
		VerificationSubmission verificationSubmission = createVerificationSubmission();
		VerificationSubmission current = createVerificationSubmission();
		VerificationState state = new VerificationState();
		state.setState(VerificationStateEnum.SUBMITTED);
		current.setStateHistory(Collections.singletonList(state));
		when(mockVerificationDao.getCurrentVerificationSubmissionForUser(USER_ID)).thenReturn(current);
		try {
			verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
			fail("exception expected");
		} catch (UnauthorizedException e) {
			// as expected
		}
		
		state.setState(VerificationStateEnum.APPROVED);
		try {
			verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
			fail("exception expected");
		} catch (UnauthorizedException e) {
			// as expected
		}

		state.setState(VerificationStateEnum.REJECTED);
		verificationManager.
			createVerificationSubmission(userInfo, verificationSubmission);

	}
	
	@Test
	public void testValidateVerificationSubmission() {
		UserProfile userProfile = createUserProfile();
		VerificationSubmission verificationSubmission = createVerificationSubmission();
		VerificationManagerImpl.validateVerificationSubmission(verificationSubmission, userProfile, ORCID);
	}



	@Test
	public void testDeleteVerificationSubmission() {
		fail("Not yet implemented");
	}

	@Test
	public void testPopulateCreateFieldsVerificationSubmissionUserInfoDate() {
		fail("Not yet implemented");
	}

	@Test
	public void testValidateField() {
		fail("Not yet implemented");
	}

	@Test
	public void testListVerificationSubmissions() {
		fail("Not yet implemented");
	}

	@Test
	public void testChangeSubmissionState() {
		fail("Not yet implemented");
	}

	@Test
	public void testPopulateCreateFieldsVerificationStateUserInfoDate() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsStateTransitionAllowed() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateSubmissionNotification() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateStateChangeNotification() {
		fail("Not yet implemented");
	}

}
