package org.sagebionetworks.repo.manager;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
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

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreateVerificationSubmission() {
		VerificationSubmission verificationSubmission = new VerificationSubmission();
		// method under test:
		verificationSubmission = verificationManager.
				createVerificationSubmission(userInfo, verificationSubmission);
		verify(mockVerificationDao).getCurrentVerificationSubmissionForUser(USER_ID);
		assertEquals(USER_ID.toString(), verificationSubmission.getCreatedBy());
		assertNotNull(verificationSubmission.getCreatedOn());
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
	public void testValidateVerificationSubmission() {
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
