package org.sagebionetworks.repo.model.auth;

public interface PasswordResetTokenDAO {

	/**
	 * Creates a new reset token for user. If an unexpired token already exists, the newly created token will replace that one.
	 * @param principalId
	 * @return
	 */
	public String createNewResetToken(long principalId);

	public Long getUserIdIfValid(String token);
}


