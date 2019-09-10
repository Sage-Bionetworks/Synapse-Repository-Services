package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class EmailClaimProviderTest {
	
	@Mock
	private UserProfileManager mockUserProfileManager;
	
	@InjectMocks
	private EmailClaimProvider emailClaimProvider;
	
	private static final String USER_ID = "101";
	
	private static final String EMAIL = "my@email.com";
	
	private UserProfile userProfile;
	
	@Before
	public void setUp() {
		userProfile = new UserProfile();
		when(mockUserProfileManager.getUserProfile(USER_ID)).thenReturn(userProfile);
		userProfile.setEmails(ImmutableList.of(EMAIL, "secondary email"));
	}

	@Test
	public void testEmailClaim() {
		assertEquals(OIDCClaimName.email, emailClaimProvider.getName());
		assertNotNull(emailClaimProvider.getDescription());
		
		// method under test
		assertEquals(EMAIL, emailClaimProvider.getClaim(USER_ID, null));
	}

	@Test
	public void testEmailClaimMissingEmail() {
		userProfile.setEmails(null);
		// method under test
		assertNull(emailClaimProvider.getClaim(USER_ID, null));
		
		userProfile.setEmails(Collections.EMPTY_LIST);
		// method under test
		assertNull(emailClaimProvider.getClaim(USER_ID, null));
	}

}
