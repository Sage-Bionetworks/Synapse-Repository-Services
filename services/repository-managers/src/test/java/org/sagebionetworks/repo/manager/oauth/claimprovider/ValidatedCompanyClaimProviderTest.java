package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

@ExtendWith(MockitoExtension.class)
public class ValidatedCompanyClaimProviderTest {
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
	@InjectMocks
	private ValidatedCompanyClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	
	private static final String COMPANY = "validated company";
	
	private VerificationSubmission verificationSubmission;
	
	@BeforeEach
	public void setUp() {
		verificationSubmission = new VerificationSubmission();
		when(mockUserProfileManager.getCurrentVerificationSubmission(Long.parseLong(USER_ID))).thenReturn(verificationSubmission);
	}

	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.validated_company, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		// method under test
		assertNull(claimProvider.getClaim(USER_ID, null, null, null));
		
		VerificationState verificationState = new VerificationState();
		verificationState.setState(VerificationStateEnum.APPROVED);
		Date now = new Date();
		verificationState.setCreatedOn(now);
		verificationSubmission.setStateHistory(Collections.singletonList(verificationState));
		verificationSubmission.setCompany(COMPANY);
		
		// method under test
		assertEquals(COMPANY, claimProvider.getClaim(USER_ID, null, null, null));
	}
}
