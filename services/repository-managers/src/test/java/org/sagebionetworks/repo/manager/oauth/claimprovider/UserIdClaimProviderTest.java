package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

@RunWith(MockitoJUnitRunner.class)
public class UserIdClaimProviderTest {
	
	@InjectMocks
	private UserIdClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	
	@Before
	public void setUp() {
	}

	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.userid, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		// method under test
		assertEquals(USER_ID, claimProvider.getClaim(USER_ID, null));
	}
}
