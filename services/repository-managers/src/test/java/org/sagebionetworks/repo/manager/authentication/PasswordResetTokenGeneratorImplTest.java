package org.sagebionetworks.repo.manager.authentication;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.authentication.PasswordResetTokenGeneratorImpl.PASSWORD_RESET_TOKEN_EXPIRATION_MILLIS;

import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.util.Clock;

@RunWith(MockitoJUnitRunner.class)
public class PasswordResetTokenGeneratorImplTest {
	@Mock
	TokenGenerator mockTokenGenerator;

	@Mock
	AuthenticationDAO mockAuthenticationDao;

	@Mock
	Clock mockClock;

	@InjectMocks
	PasswordResetTokenGeneratorImpl passwordResetTokenGenerator;

	PasswordResetSignedToken token;

	final long userId = 1337;
	final String passwordHash = "Somebody once told me\n" +
			"The world is gonna roll me\n" +
			"I ain't the sharpest tool in the shed";
	final String sha256OfPasswordHash = DigestUtils.sha256Hex(passwordHash);

	final long currentTime = 420;

	@Before
	public void setUp(){
		when(mockClock.currentTimeMillis()).thenReturn(currentTime);
		token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(userId));
		token.setCreatedOn(new Date(currentTime));
		token.setExpiresOn(new Date(currentTime + PASSWORD_RESET_TOKEN_EXPIRATION_MILLIS));
		token.setUserId("1337");
		token.setValidity(sha256OfPasswordHash);

		when(mockAuthenticationDao.getPasswordHash(userId)).thenReturn(passwordHash);
	}

	@Test
	public void testGetToken(){

		//method under test
		PasswordResetSignedToken generatedToken = passwordResetTokenGenerator.getToken(userId);

		verify(mockTokenGenerator).signToken(generatedToken);
		assertEquals(token, generatedToken);
	}


	@Test
	public void testIsValidToken_Invalid_NonMatchingHMAC(){
		doThrow(UnauthorizedException.class).when(mockTokenGenerator).validateToken(token);

		//method under test
		assertFalse(passwordResetTokenGenerator.isValidToken(token));

		verifyZeroInteractions(mockAuthenticationDao);
		verify(mockTokenGenerator).validateToken(token);
	}


	@Test
	public void testIsValidToken_Invalid_NonMatchingValidityHash(){
		when(mockAuthenticationDao.getPasswordHash(userId)).thenReturn("defintely not the same password hash");

		//method under test
		assertFalse(passwordResetTokenGenerator.isValidToken(token));

		verify(mockTokenGenerator).validateToken(token);
		verify(mockAuthenticationDao).getPasswordHash(userId);
	}


	@Test
	public void testIsValidToken_valid(){

		//method under test
		assertTrue(passwordResetTokenGenerator.isValidToken(token));

		verify(mockTokenGenerator).validateToken(token);
		verify(mockAuthenticationDao).getPasswordHash(userId);
	}

	@Test
	public void testCreateValidityHash_NullPasswordHash(){
		when(mockAuthenticationDao.getPasswordHash(userId)).thenReturn(null);
		assertNotNull(passwordResetTokenGenerator.createValidityHash(userId));
	}
}
