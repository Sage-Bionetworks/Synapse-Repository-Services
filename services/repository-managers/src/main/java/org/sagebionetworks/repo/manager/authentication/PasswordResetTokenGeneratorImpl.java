package org.sagebionetworks.repo.manager.authentication;

import java.util.Base64;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;

public class PasswordResetTokenGeneratorImpl implements PasswordResetTokenGenerator {
	@Autowired
	TokenGenerator tokenGenerator;

	@Autowired
	AuthenticationDAO authenticationDAO;

	@Autowired
	Clock clock;

	public static final long PASSWORD_RESET_TOKEN_EXPIRATION_MILLIS = 24 * 60 * 60 * 1000; //24 hours


	@Override
	public PasswordResetSignedToken getToken(long userId){
		PasswordResetSignedToken token = createUnsignedToken(userId);
		tokenGenerator.signToken(token);
		return token;
	}

	@Override
	public boolean isValidToken(PasswordResetSignedToken token){
		try {
			tokenGenerator.validateToken(token);
		} catch (UnauthorizedException e){
			return false;
		}
		String currentValidityHash = createValidityHash(Long.parseLong(token.getUserId()));
		return currentValidityHash.equals(token.getValidity());
	}

	PasswordResetSignedToken createUnsignedToken(long userId){
		final long now = clock.currentTimeMillis();
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(userId));
		token.setCreatedOn(new Date(now));
		token.setExpiresOn(new Date(now + PASSWORD_RESET_TOKEN_EXPIRATION_MILLIS));
		token.setValidity(createValidityHash(userId));

		return token;
	}

	String createValidityHash(long userId){
		/*
		We hash the current password hash
		This ensures that in the event of a password change the token becomes invalid.
		Password hash changes even if it is changed to the exact same password since the salt is randomly generated on every password change
		 */
		String data = authenticationDAO.getPasswordHash(userId);
		return DigestUtils.sha256Hex(data != null ? data : "");
	}


}
