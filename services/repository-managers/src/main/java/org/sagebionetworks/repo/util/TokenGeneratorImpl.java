package org.sagebionetworks.repo.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Date;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.securitytools.HMACUtils;

public class TokenGeneratorImpl implements TokenGenerator {

	private static final String UTF_8 = Charset.forName("utf-8").name();

	/**
	 * Tokens currently expire after 30 days.
	 */
	public static final long TOKEN_EXPIRATION_MS = 1000L * 60L * 60L * 24L * 30L;
	/**
	 * The default key version used to validate keys old tokens without key versions.
	 */
	public static final int DEFAULT_KEY_VERSION = 0;
	
	/**
	 * All tokens without an expiration will expire on Thursday, July 19, 2018 12:00:00 AM GMT
	 */
	public static final long OLD_TOKEN_EXPIRATION_EPOCH_MS = 1531958400000L;

	StackConfiguration configuration;
	Clock clock;
	
	/**
	 * The only constructor includes all dependencies.
	 * @param configuration
	 * @param clock
	 */
	public TokenGeneratorImpl(StackConfiguration configuration, Clock clock) {
		super();
		this.configuration = configuration;
		this.clock = clock;
	}

	@Override
	public void signToken(SignedTokenInterface token) {
		// use the current key version to sign this token.
		int keyVersion = configuration.getCurrentHmacSigningKeyVersion();
		String hmac = generateSignature(token, keyVersion);
		token.setHmac(hmac);
		token.setVersion((long) keyVersion);
		long expires = clock.currentTimeMillis() + TOKEN_EXPIRATION_MS;
		token.setExpiresOn(new Date(expires));
	}

	@Override
	public void validateToken(SignedTokenInterface token) throws UnauthorizedException {
		String hmac = token.getHmac();
		token.setHmac(null);
		// is the token expired?
		if(isExpired(token)) {
			throw new UnauthorizedException("Token has expired.");
		}
		int keyVersion = DEFAULT_KEY_VERSION;
		if(token.getVersion() != null) {
			keyVersion = token.getVersion().intValue();
		}
		String regeneratedHmac = generateSignature(token, keyVersion);
		if (!regeneratedHmac.equals(hmac)) {
			throw new UnauthorizedException("Token signature is invalid.");
		}
		// restore the starting HMAC.
		token.setHmac(hmac);
	}

	String generateSignature(SignedTokenInterface token, int keyVersion) {
		if (token.getHmac() != null) {
			throw new IllegalArgumentException("HMAC is added only after generating signature.");
		}
		try {
			String jsonString = EntityFactory.createJSONStringForEntity(token);
			byte[] secretKey = configuration.getHmacSigningKeyForVersion(keyVersion).getBytes(UTF_8);
			byte[] signatureAsBytes = HMACUtils.generateHMACSHA1SignatureFromRawKey(jsonString, secretKey);
			return new String(signatureAsBytes, UTF_8);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Is the given token expired?
	 * @param token
	 * @return
	 */
	@Override
	public boolean isExpired(SignedTokenInterface token) {
		if(token.getExpiresOn() == null) {
			// All old tokens without an expiration will expires on July 18th 2018.
			return clock.currentTimeMillis() > OLD_TOKEN_EXPIRATION_EPOCH_MS;
		}else {
			return clock.currentTimeMillis() > token.getExpiresOn().getTime();
		}
	}

}
