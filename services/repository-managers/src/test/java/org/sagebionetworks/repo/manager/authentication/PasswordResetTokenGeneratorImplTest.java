package org.sagebionetworks.repo.manager.authentication;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.authentication.PasswordResetTokenGeneratorImpl.PASSWORD_RESET_TOKEN_EXPIRATION_MILLIS;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
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

	final long userId = 1337;
	final String passwordHash = "Somebody once told me\n" +
			"The world is gonna roll me\n" +
			"I ain't the sharpest tool in the shed";

	final long currentTime = 420;

	@Before
	public void setUp(){
		when(mockClock.currentTimeMillis()).thenReturn(currentTime);
	}

	@Test
	public void testGetToken(){
		PasswordResetSignedToken expectedToken = new PasswordResetSignedToken();
		expectedToken.setCreatedOn(new Date(currentTime));
		expectedToken.setExpiresOn(new Date(currentTime + PASSWORD_RESET_TOKEN_EXPIRATION_MILLIS));
		expectedToken.setUserId("1337");
		expectedToken.setValidity("e1c00300dc7f147b3ecf9ecc8d1dc6ddbb095da3262fe588f03e965334bad724");

		when(mockAuthenticationDao.getPasswordHash(userId)).thenReturn(passwordHash);

		PasswordResetSignedToken generatedToken = passwordResetTokenGenerator.getToken(userId);

		verify(mockTokenGenerator).signToken(generatedToken);
		assertEquals(expectedToken, generatedToken);
	}

	//TODO:

	@Test
	public void testIsValidToken_Invalid_NonMatchingHMAC(){

	}

	@Test
	public void testIsValidToken_Invalid_NonMatchingValidityHash(){

	}


	@Test
	public void testIsValidToken_valid(){

	}
}
