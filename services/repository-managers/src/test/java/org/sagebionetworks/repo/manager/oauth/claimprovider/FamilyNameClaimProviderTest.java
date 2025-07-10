package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

@ExtendWith(MockitoExtension.class)
public class FamilyNameClaimProviderTest {
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
	@InjectMocks
	private FamilyNameClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	
	private static final String LAST_NAME = "Lastname";
	
	private UserProfile userProfile;
	
	@BeforeEach
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
		assertEquals(LAST_NAME, claimProvider.getClaim(USER_ID, null, null, null));
	}

	@Test
	public void testClaimMissing() {
		userProfile.setLastName(null);
		// method under test
		assertNull(claimProvider.getClaim(USER_ID, null, null, null));
	}

}
