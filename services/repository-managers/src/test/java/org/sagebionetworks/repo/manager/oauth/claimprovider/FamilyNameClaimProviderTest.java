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
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

@RunWith(MockitoJUnitRunner.class)
public class FamilyNameClaimProviderTest {
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
	@InjectMocks
	private FamilyNameClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	
	private static final String LAST_NAME = "Lastname";
	
	private UserProfile userProfile;
	
	@Before
	public void setUp() {
		userProfile = new UserProfile();
		when(mockUserProfileManager.getUserProfile(USER_ID)).thenReturn(userProfile);
		userProfile.setLastName(LAST_NAME);
	}

	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.family_name, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		// method under test
		assertEquals(LAST_NAME, claimProvider.getClaim(USER_ID, null));
	}

	@Test
	public void testClaimMissing() {
		userProfile.setLastName(null);
		// method under test
		assertNull(claimProvider.getClaim(USER_ID, null));
	}

}
