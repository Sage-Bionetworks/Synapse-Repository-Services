package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class ValidatedEmailClaimProviderTest {
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
	@Mock
	private NotificationEmailDAO mockNotificationEmailDao;
	
	@InjectMocks
	private ValidatedEmailClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	
	private static final String EMAIL = "validated@email.com";
	
	private VerificationSubmission verificationSubmission;
	
	@Before
	public void setUp() {
		verificationSubmission = new VerificationSubmission();
		VerificationState verificationState = new VerificationState();
		verificationState.setState(VerificationStateEnum.APPROVED);
		Date now = new Date();
		verificationState.setCreatedOn(now);
		verificationSubmission.setStateHistory(Collections.singletonList(verificationState));
	}

	@Test
	public void testClaimConfig() {
		// method under test
		assertEquals(OIDCClaimName.validated_email, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
	}
	
	@Test
	public void testNoVerificationSubmission() {
		// method under test
		assertNull(claimProvider.getClaim(USER_ID, null));
	}
	
	@Test
	public void testHappyCase() {
		verificationSubmission.setNotificationEmail(EMAIL);
		when(mockUserProfileManager.getCurrentVerificationSubmission(Long.parseLong(USER_ID))).thenReturn(verificationSubmission);
		
		// method under test
		assertEquals(EMAIL, claimProvider.getClaim(USER_ID, null));
	
	}
	
	@Test
	public void testNoNotificationEmailOneAliasEmail() {
		verificationSubmission.setNotificationEmail(null);
		verificationSubmission.setEmails(Collections.singletonList(EMAIL));
		when(mockUserProfileManager.getCurrentVerificationSubmission(Long.parseLong(USER_ID))).thenReturn(verificationSubmission);
		
		// method under test
		assertEquals(EMAIL, claimProvider.getClaim(USER_ID, null));
	
	}
	
	@Test
	public void testNoNotificationEmailMultipleAliasEmailsCANDisambiguate() {
		verificationSubmission.setNotificationEmail(null);
		verificationSubmission.setEmails(ImmutableList.of(EMAIL, "some other address"));
		when(mockUserProfileManager.getCurrentVerificationSubmission(Long.parseLong(USER_ID))).thenReturn(verificationSubmission);
		
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(Long.parseLong(USER_ID))).thenReturn(EMAIL);
		
		// method under test
		assertEquals(EMAIL, claimProvider.getClaim(USER_ID, null));
	
	}	
	@Test
	public void testNoNotificationEmailMultipleAliasEmailsCanNOTDisambiguate() {
		verificationSubmission.setNotificationEmail(null);
		verificationSubmission.setEmails(ImmutableList.of(EMAIL, "some other address"));
		when(mockUserProfileManager.getCurrentVerificationSubmission(Long.parseLong(USER_ID))).thenReturn(verificationSubmission);
		
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(Long.parseLong(USER_ID))).thenReturn("doen't match either!");
		
		// method under test
		assertEquals(EMAIL, claimProvider.getClaim(USER_ID, null));
	
	}
}
