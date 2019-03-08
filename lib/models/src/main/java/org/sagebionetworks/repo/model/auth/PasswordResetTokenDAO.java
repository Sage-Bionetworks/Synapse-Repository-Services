package org.sagebionetworks.repo.model.auth;

public interface PasswordResetTokenDAO {

	/**
	 * Creates a new reset token for user. If an unexpired token already exists, the newly created token will replace that one.
	 * @param principalId
	 * @return
	 */
	public String createOrRefreshResetToken(long principalId, long expirationDurationMillis);

	/**
	 * If the token has not already expired, use the hash of the reset token to find the user's Id.
	 * @param tokenHash
	 * @return
	 */
	public Long getUserIdIfValidHash(String tokenHash);

	public void nullifyToken(long userId);
}


