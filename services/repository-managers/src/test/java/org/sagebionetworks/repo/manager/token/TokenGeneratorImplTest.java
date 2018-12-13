package org.sagebionetworks.repo.manager.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.manager.token.TokenGeneratorImpl.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.NewUserSignedToken;


@RunWith(MockitoJUnitRunner.class)
public class TokenGeneratorImplTest {
	
	@Mock
	StackConfiguration mockConfig;
	@Mock
	Clock mockClock;
	
	NewUserSignedToken token;

	TokenGeneratorImpl generator;
	
	int currentKeyVersion;
	
	@Before
	public void before() {
		token = new NewUserSignedToken();
		token.setFirstName("firstName");
		generator = new TokenGeneratorImpl(mockConfig, mockClock);
		when(mockConfig.getHmacSigningKeyForVersion(0)).thenReturn("key-zero");
		when(mockConfig.getHmacSigningKeyForVersion(1)).thenReturn("key-one");
		currentKeyVersion = 1;
		when(mockConfig.getCurrentHmacSigningKeyVersion()).thenReturn(currentKeyVersion);
		
		when(mockClock.currentTimeMillis()).thenReturn(1L,2L,3L,4L);
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
	public void testGenerateSignature() {
		int keyVersion = 0;
		// call under test
		String signature = generator.generateSignature(token, keyVersion);
		assertNotNull(signature);
		verify(mockConfig).getHmacSigningKeyForVersion(keyVersion);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGenerateSignatureWithHMAC() {
		token.setHmac("should not be sets");
		int keyVersion = 0;
		// call under test
		generator.generateSignature(token, keyVersion);
	}
	
	@Test
	public void testSignTokenNoExpires() {
		token.setExpiresOn(null);
		// call under test
		generator.signToken(token);
		assertEquals(new Long(currentKeyVersion), token.getVersion());
		assertNotNull(token.getExpiresOn());
		assertEquals(TOKEN_EXPIRATION_MS+1L, token.getExpiresOn().getTime());
		assertNotNull(token.getHmac());
		// the current version should be used.
		verify(mockConfig).getCurrentHmacSigningKeyVersion();
	}
	
	@Test
	public void testSignTokenWithExpires() {
		// should use the provided expiration
		token.setExpiresOn(new Date(3L));
		// call under test
		generator.signToken(token);
		assertEquals(new Long(currentKeyVersion), token.getVersion());
		assertNotNull(token.getExpiresOn());
		assertEquals(3L, token.getExpiresOn().getTime());
		assertNotNull(token.getHmac());
	}
	
	@Test
	public void testSignTokenWithExpiresPLFM_4958() {
		// should use the provided expiration
		token.setExpiresOn(new Date(TOKEN_EXPIRATION_MS+10));
		// call under test
		try {
			generator.signToken(token);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("PLFM-4958"));
		}
	}
	
	@Test
	public void testValidateToken() {
		// sing the token
		generator.signToken(token);
		// call under test
		generator.validateToken(token);
		verify(mockConfig, times(2)).getHmacSigningKeyForVersion(currentKeyVersion);
		assertNotNull(token.getHmac());
		assertNotNull(token.getExpiresOn());
		assertNotNull(token.getVersion());
	}
	
	@Test
	public void testValidateTokenModified() {
		// sing the token
		generator.signToken(token);
		// changing the token after it is signed should invalidate it.
		token.setLastName("tamper");
		try {
			// call under test
			generator.validateToken(token);
			fail();
		} catch (UnauthorizedException e) {
			// expected
			assertEquals(TOKEN_SIGNATURE_IS_INVALID, e.getMessage());
		}
	}
	
	@Test
	public void testValidateTokenExpired() {
		// sing the token
		generator.signToken(token);
		// move the clock forward past expiration
		when(mockClock.currentTimeMillis()).thenReturn(TOKEN_EXPIRATION_MS+2);
		try {
			// call under test
			generator.validateToken(token);
			fail();
		} catch (UnauthorizedException e) {
			// expected
			assertEquals(TOKEN_HAS_EXPIRED, e.getMessage());
		}
	}
	
	/**
	 * Prior to PLFM-4958, tokens did not have an expiresOn or version.
	 * All such tokens will be treated as expired after July 2018.
	 * To test this case both version and expiration fields are null.
	 */
	@Test
	public void testValidateTokenOldTokenBeforeJuly() {
		token.setExpiresOn(null);
		token.setVersion(null);
		String hmac = generator.generateSignature(token, DEFAULT_KEY_VERSION);
		token.setHmac(hmac);

		// move the clock past July 2018
		when(mockClock.currentTimeMillis()).thenReturn(OLD_TOKEN_EXPIRATION_EPOCH_MS-1);
		// call under test
		generator.validateToken(token);
		// default key should be used.
		verify(mockConfig, times(2)).getHmacSigningKeyForVersion(DEFAULT_KEY_VERSION);
	}
	
	/**
	 * Prior to PLFM-4958, tokens would not expire and had no version.
	 * All such tokens will be treated as expired after July 2018.
	 * To test this case both version and expiration fields are cleared.
	 * 
	 */
	@Test
	public void testValidateTokenOldTokenAfterJuly() {
		token.setExpiresOn(null);
		token.setVersion(null);
		String hmac = generator.generateSignature(token, DEFAULT_KEY_VERSION);
		token.setHmac(hmac);
		
		// move the clock before July 2018
		when(mockClock.currentTimeMillis()).thenReturn(OLD_TOKEN_EXPIRATION_EPOCH_MS+1);
		try {
			// call under test
			generator.validateToken(token);
			fail();
		} catch (UnauthorizedException e) {
			// expected
			assertEquals(TOKEN_HAS_EXPIRED, e.getMessage());
		}
	}
	
}
