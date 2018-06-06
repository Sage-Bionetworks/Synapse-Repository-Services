package org.sagebionetworks.repo.manager.util;

import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.repo.model.auth.NewUserSignedToken;
import org.sagebionetworks.repo.util.TokenGeneratorImpl;

import static org.sagebionetworks.repo.util.TokenGeneratorImpl.*;

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
