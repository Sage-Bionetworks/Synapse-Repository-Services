package org.sagebionetworks.repo.manager.authentication;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.AuthenticationReceiptToken;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class AuthenticationReceiptTokenGeneratorImpl implements AuthenticationReceiptTokenGenerator {

	public static final long TOKEN_EXPIRATION_MILLIS = 3 * 24 * 60 * 60 * 1000; // 3 days

	@Autowired
	private TokenGenerator tokenGenerator;

	@Autowired
	private Clock clock;

	@Override
	public boolean isReceiptValid(long principalId, String authenticationReceipt) {
		if (authenticationReceipt == null) {
			return false;
		}
		try {
			String decodedToken = new String(Base64.getDecoder().decode(authenticationReceipt.getBytes(StandardCharsets.UTF_8)),
					StandardCharsets.UTF_8);
			AuthenticationReceiptToken token = EntityFactory.createEntityFromJSONString(decodedToken,
					AuthenticationReceiptToken.class);
			if(token.getUserId() == null) {
				return false;
			}
			if(!token.getUserId().equals(principalId)) {
				return false;
			}
			if(token.getExpiresOn() == null) {
				return false;
			}
			tokenGenerator.validateToken(token);
			return true;
		} catch (JSONObjectAdapterException | UnauthorizedException | IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public String createNewAuthenticationReciept(long principalId) {
		final long now = clock.currentTimeMillis();
		AuthenticationReceiptToken token = new AuthenticationReceiptToken();
		token.setUserId(principalId);
		token.setExpiresOn(new Date(now + TOKEN_EXPIRATION_MILLIS));
		tokenGenerator.signToken(token);
		try {
			String tokenJson = EntityFactory.createJSONStringForEntity(token);
			return new String(Base64.getEncoder().encode(tokenJson.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}
	
}
