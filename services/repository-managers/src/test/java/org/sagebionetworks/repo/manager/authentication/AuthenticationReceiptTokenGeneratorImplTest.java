package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.AuthenticationReceiptToken;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.Clock;


@ExtendWith(MockitoExtension.class)
public class AuthenticationReceiptTokenGeneratorImplTest {

	@Mock
	private TokenGenerator mockTokenGenerator;

	@Mock
	private Clock mockClock;

	@InjectMocks
	private AuthenticationReceiptTokenGeneratorImpl authenticationReceiptTokenGenerator;

	long principalId;
	String hmac;
	long versionNumber;
	AuthenticationReceiptToken token;

	@BeforeEach
	public void before() {
		principalId = 54321L;
		hmac = "some-hmac";
		versionNumber = 12L;
		token = new AuthenticationReceiptToken();
		token.setUserId(principalId);
		token.setExpiresOn(new Date(1L + AuthenticationReceiptTokenGeneratorImpl.TOKEN_EXPIRATION_MILLIS));
		token.setHmac(hmac);
		token.setVersion(versionNumber);
	}

	@Test
	public void testCreateNewAuthenticationReciept() {
		when(mockClock.currentTimeMillis()).thenReturn(1L, 2L);
		setupSignToken();
		long principalId = 54321L;
		// call under test
		String tokenString = authenticationReceiptTokenGenerator.createNewAuthenticationReciept(principalId);
		assertNotNull(tokenString);

		AuthenticationReceiptToken decoded = decodeToken(tokenString);
		assertEquals(token, decoded);
		verify(mockTokenGenerator).signToken(token);
	}

	@Test
	public void testIsReceiptValid() {
		String encodedToken = encodeToken(token);
		// call under test
		boolean isValid = authenticationReceiptTokenGenerator.isReceiptValid(principalId, encodedToken);
		assertTrue(isValid);
		verify(mockTokenGenerator).validateToken(token);
	}
	
	@Test
	public void testIsReceiptValidWithNullUserId() {
		token.setUserId(null);
		String encodedToken = encodeToken(token);
		// call under test
		boolean isValid = authenticationReceiptTokenGenerator.isReceiptValid(principalId, encodedToken);
		assertFalse(isValid);
		verify(mockTokenGenerator, never()).validateToken(any());
	}
	
	@Test
	public void testIsReceiptValidWithWrongUserId() {
		token.setUserId(principalId+1);
		String encodedToken = encodeToken(token);
		// call under test
		boolean isValid = authenticationReceiptTokenGenerator.isReceiptValid(principalId, encodedToken);
		assertFalse(isValid);
		verify(mockTokenGenerator, never()).validateToken(any());
	}
	
	@Test
	public void testIsReceiptValidWithNullExpiredOn() {
		token.setExpiresOn(null);
		String encodedToken = encodeToken(token);
		// call under test
		boolean isValid = authenticationReceiptTokenGenerator.isReceiptValid(principalId, encodedToken);
		assertFalse(isValid);
		verify(mockTokenGenerator, never()).validateToken(any());
	}
	
	@Test
	public void testIsReceiptValidWithNull() {
		String encodedToken = null;
		// call under test
		boolean isValid = authenticationReceiptTokenGenerator.isReceiptValid(principalId, encodedToken);
		assertFalse(isValid);
		verify(mockTokenGenerator, never()).validateToken(any());
	}
	
	@Test
	public void testIsReceiptValidWithNotBase64() {
		String encodedToken = "$%^@@#$";
		// call under test
		boolean isValid = authenticationReceiptTokenGenerator.isReceiptValid(principalId, encodedToken);
		assertFalse(isValid);
		verify(mockTokenGenerator, never()).validateToken(any());
	}
	
	@Test
	public void testIsReceiptValidWithNotEncodedJSON() {
		String encodedToken = new String(Base64.getEncoder().encode("not JSON".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);;
		// call under test
		boolean isValid = authenticationReceiptTokenGenerator.isReceiptValid(principalId, encodedToken);
		assertFalse(isValid);
		verify(mockTokenGenerator, never()).validateToken(any());
	}
	
	@Test
	public void testIsReceiptValidWithNotValid() {
		doThrow(new UnauthorizedException("not valid")).when(mockTokenGenerator).validateToken(token);
		String encodedToken = encodeToken(token);
		// call under test
		boolean isValid = authenticationReceiptTokenGenerator.isReceiptValid(principalId, encodedToken);
		assertFalse(isValid);
		verify(mockTokenGenerator).validateToken(token);
	}

	/**
	 * Helper to decode a token string
	 * 
	 * @param token
	 * @return
	 */
	AuthenticationReceiptToken decodeToken(String token) {
		try {
			String decodedToken = new String(Base64.getDecoder().decode(token.getBytes(StandardCharsets.UTF_8)),
					StandardCharsets.UTF_8);
			return EntityFactory.createEntityFromJSONString(decodedToken, AuthenticationReceiptToken.class);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Helper to encode a token
	 * 
	 * @param token
	 * @return
	 */
	String encodeToken(AuthenticationReceiptToken token) {
		try {
			String tokenJson = EntityFactory.createJSONStringForEntity(token);
			return new String(Base64.getEncoder().encode(tokenJson.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}

	void setupSignToken() {
		doAnswer((InvocationOnMock a) -> {
			SignedTokenInterface token = a.getArgument(0);
			token.setHmac(hmac);
			token.setVersion(versionNumber);
			return null;
		}).when(mockTokenGenerator).signToken(any());
	}

}
