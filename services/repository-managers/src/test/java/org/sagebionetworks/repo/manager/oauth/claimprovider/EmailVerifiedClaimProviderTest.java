package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

@RunWith(MockitoJUnitRunner.class)
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
		assertTrue((Boolean)claimProvider.getClaim("101", null));
	}

}
