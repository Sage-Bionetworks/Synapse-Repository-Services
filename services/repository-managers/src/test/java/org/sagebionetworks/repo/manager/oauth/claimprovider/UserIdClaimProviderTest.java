package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

@ExtendWith(MockitoExtension.class)
public class UserIdClaimProviderTest {
	
	@InjectMocks
	private UserIdClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	

	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.userid, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		// method under test
		assertEquals(USER_ID, claimProvider.getClaim(USER_ID, null, null, null));
	}
}
