package org.sagebionetworks.repo.manager.token;

import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.repo.model.UnauthorizedException;

/**
 * Abstraction for generating HMAC signed tokens.
 *
 */
public interface TokenGenerator {

	/**
	 * Sign the given token using the current version of the signing key.
	 * 
	 * @param token
	 */
	public void signToken(SignedTokenInterface token);

	/**
	 * Validate the signature of the passed token.
	 * 
	 * @param token
	 * @throws UnauthorizedException When the HMAC signature does not match or if the token is expired.
	 */
	public void validateToken(SignedTokenInterface token) throws UnauthorizedException;

	/**
	 * Is the given token expired?
	 * 
	 * @param token
	 * @return
	 */
	boolean isExpired(SignedTokenInterface token);
}
