package org.sagebionetworks.repo.manager.authentication;

import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;

public interface PasswordResetTokenGenerator {
	PasswordResetSignedToken getToken(long userId);

	boolean isValidToken(PasswordResetSignedToken token);
}
