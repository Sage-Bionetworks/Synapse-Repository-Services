package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

@RunWith(MockitoJUnitRunner.class)
public class IsValidatedClaimProviderTest {
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
	@InjectMocks
	private IsValidatedClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	
	private VerificationSubmission verificationSubmission;
	
	@Before
	public void setUp() {
		verificationSubmission = new VerificationSubmission();
		when(mockUserProfileManager.getCurrentVerificationSubmission(Long.parseLong(USER_ID))).thenReturn(verificationSubmission);
	}

	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.is_validated, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		// method under test
		assertFalse((Boolean)claimProvider.getClaim(USER_ID, null));
		
		VerificationState verificationState = new VerificationState();
		verificationState.setState(VerificationStateEnum.APPROVED);
		Date now = new Date();
		verificationState.setCreatedOn(now);
		verificationSubmission.setStateHistory(Collections.singletonList(verificationState));
		
		// method under test
		assertTrue((Boolean)claimProvider.getClaim(USER_ID, null));
	}
}
