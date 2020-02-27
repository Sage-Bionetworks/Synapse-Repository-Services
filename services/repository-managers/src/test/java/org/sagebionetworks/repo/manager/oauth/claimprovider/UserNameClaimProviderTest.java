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
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;

@RunWith(MockitoJUnitRunner.class)
public class UserNameClaimProviderTest {
	
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDao;
	
	@InjectMocks
	private UserNameClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	private static final long USER_ID_LONG = Long.parseLong(USER_ID);
	
	private static final String USER_NAME = "myUserName";
	
	@Test
	public void testEmailClaim() {
		when(mockPrincipalAliasDao.getUserName(USER_ID_LONG)).thenReturn(USER_NAME);
				
		// method under test
		assertEquals(OIDCClaimName.user_name, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		// method under test
		assertEquals(USER_NAME, claimProvider.getClaim(USER_ID, null));
	}

	@Test
	public void testEmailClaimMissingEmail() {
		when(mockPrincipalAliasDao.getUserName(USER_ID_LONG)).thenReturn(null);
		// method under test
		assertNull(claimProvider.getClaim(USER_ID, null));
	}

}
