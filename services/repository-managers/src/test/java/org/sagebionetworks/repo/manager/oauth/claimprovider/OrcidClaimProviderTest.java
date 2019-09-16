package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

@RunWith(MockitoJUnitRunner.class)
public class OrcidClaimProviderTest {
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
	@InjectMocks
	private OrcidClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	
	private static final String ORCID = "orcid";
	
	@Before
	public void setUp() {
		when(mockUserProfileManager.getOrcid(Long.parseLong(USER_ID))).thenReturn(ORCID);
	}

	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.orcid, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		// method under test
		assertEquals(ORCID, claimProvider.getClaim(USER_ID, null));
	}

	@Test
	public void testClaimMissing() {
		when(mockUserProfileManager.getOrcid(Long.parseLong(USER_ID))).thenReturn(null);
		// method under test
		assertNull(claimProvider.getClaim(USER_ID, null));
	}

}
