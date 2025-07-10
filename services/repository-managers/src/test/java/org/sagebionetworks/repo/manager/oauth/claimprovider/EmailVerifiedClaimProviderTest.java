package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

@ExtendWith(MockitoExtension.class)
public class EmailVerifiedClaimProviderTest {

	@InjectMocks
	private EmailVerifiedClaimProvider claimProvider;


	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.email_verified, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());	
		// method under test
		assertTrue((Boolean)claimProvider.getClaim("101", null, null, null));
	}

}
