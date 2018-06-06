package org.sagebionetworks.repo.util;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.ClockImpl;
import org.sagebionetworks.repo.model.SignedTokenInterface;

/**
 * Utility to proved static access to TokenGenerator.
 * 
 * @deprecated use: @see {@link TokenGenerator}
 */
public class SignedTokenUtil {

	/**
	 * Provided as a singleton to support old static calls.
	 */
	private static final TokenGenerator tokenGenerator;
	static {
		tokenGenerator = new TokenGeneratorImpl(StackConfiguration.singleton(), new ClockImpl());
	}

	/**
	 * Add the HMAC
	 * 
	 * @param token
	 * @return the signed, serialized token
	 * @deprecated use: @see {@link TokenGenerator}
	 */
	public static void signToken(SignedTokenInterface token) {
		tokenGenerator.signToken(token);
	}

	/**
	 * validate the HMAC
	 * 
	 * @param token
	 * @return
	 * 
	 * @deprecated use: @see {@link TokenGenerator}
	 */
	public static void validateToken(SignedTokenInterface token) {
		tokenGenerator.validateToken(token);
	}

}
