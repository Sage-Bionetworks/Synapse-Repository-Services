package org.sagebionetworks.repo.manager.token;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.token.TokenGeneratorImpl.OLD_TOKEN_EXPIRATION_EPOCH_MS;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.repo.model.auth.NewUserSignedToken;

@RunWith(MockitoJUnitRunner.class)
public class TokenGeneratorImplTest {
	
	@Mock
	StackConfiguration mockConfig;
	@Mock
	Clock mockClock;
	
	SignedTokenInterface token;

	TokenGeneratorImpl generator;
	
	@Before
	public void before() {
		token = new NewUserSignedToken();
		generator = new TokenGeneratorImpl(mockConfig, mockClock);
	}
	
	@Test
	public void testIsExpiredWithNullExpiredBeforeEnd() {
		// current time is before the old token expiration.
		when(mockClock.currentTimeMillis()).thenReturn(OLD_TOKEN_EXPIRATION_EPOCH_MS-1L);
		// token without expires on
		token.setExpiresOn(null);
		assertFalse(generator.isExpired(token));
	}
	
	@Test
	public void testIsExpiredWithNullExpiredAfterEnd() {
		// current time is before the old token expiration.
		when(mockClock.currentTimeMillis()).thenReturn(OLD_TOKEN_EXPIRATION_EPOCH_MS+1L);
		// token without expires on
		token.setExpiresOn(null);
		assertTrue(generator.isExpired(token));
	}
	
	@Test
	public void testIsExpiredNotExpired() {
		// current time is before the old token expiration.
		when(mockClock.currentTimeMillis()).thenReturn(OLD_TOKEN_EXPIRATION_EPOCH_MS+1L);
		// token without expires on
		token.setExpiresOn(null);
		assertTrue(generator.isExpired(token));
	}
}
