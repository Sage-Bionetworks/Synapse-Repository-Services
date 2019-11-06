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

@RunWith(MockitoJUnitRunner.class)
public class EmailClaimProviderTest {
	
	@Mock
	private NotificationEmailDAO mockNotificationEmailDao;
	
	@InjectMocks
	private EmailClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	private static final long USER_ID_LONG = Long.parseLong(USER_ID);
	
	private static final String EMAIL = "my@email.com";
	
	@Test
	public void testEmailClaim() {
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(EMAIL);
		// method under test
		assertEquals(OIDCClaimName.email, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		// method under test
		assertEquals(EMAIL, claimProvider.getClaim(USER_ID, null));
	}

	@Test
	public void testEmailClaimMissingEmail() {
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID_LONG)).thenReturn(null);
		// method under test
		assertNull(claimProvider.getClaim(USER_ID, null));
	}

}
