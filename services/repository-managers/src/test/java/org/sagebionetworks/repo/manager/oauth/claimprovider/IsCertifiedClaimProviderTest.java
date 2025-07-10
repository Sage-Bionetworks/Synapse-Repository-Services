package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;

@ExtendWith(MockitoExtension.class)
public class IsCertifiedClaimProviderTest {
	
	@Mock
	private UserManager mockUserManager;
	
	@InjectMocks
	private IsCertifiedClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	
	private static final UserInfo USER_INFO = new UserInfo(false);
	
	@BeforeEach
	public void setUp() {
		USER_INFO.setGroups(Collections.EMPTY_SET);
		when(mockUserManager.getUserInfo(Long.parseLong(USER_ID))).thenReturn(USER_INFO);
	}

	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.is_certified, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		// method under test
		assertFalse((Boolean)claimProvider.getClaim(USER_ID, null, null, null));
		USER_INFO.setGroups(Collections.singleton(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		// method under test
		assertTrue((Boolean)claimProvider.getClaim(USER_ID, null, null, null));
	}
}
