package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthDisableRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthLoginRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthResetRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TwoFactorAuthRequiredException;


public interface AuthenticationManager {	
	/**
	 * Set a user's password without any authorization checks
	 */
	void setPassword(Long principalId, String password);

	/**
	 * Change a user's password after checking the validity of the request by checking the user's old password
	 * @return id of the user whose password has been changed
	 */
	long changePassword(ChangePasswordInterface changePasswordWithOldPassword);

	/**
	 * Gets the user's secret key
	 */
	String getSecretKey(Long principalId) throws NotFoundException;
	
	/**
	 * Replaces the user's secret key with a new one
	 */
	void changeSecretKey(Long principalId);
	
	/**
	 * Creates a token tha can be used to reset a user's password
	 */
	PasswordResetSignedToken createPasswordResetToken(long principalId) throws NotFoundException;
	
	/**
	 * Returns whether the user has accepted the terms of use
	 */
	boolean hasUserAcceptedTermsOfUse(Long id) throws NotFoundException;

	/**
	 * Sets whether the user has accepted or rejected the terms of use
	 */
	void signTermsOfUser(Long principalId);

	/**
	 * Get the date/time the user was last logged in
	 * 
	 * @param userInfo
	 * @return
	 */
	AuthenticatedOn getAuthenticatedOn(UserInfo userInfo);

	/**
	 * Log user in using information from the LoginRequest, if the user has 2FA enabled TwoFactorAuthRequiredException is thrown.
	 * 
	 * @param request
	 * @param tokenIssuer
	 * @return
	 * @throws TwoFactorAuthRequiredException If the user has 2FA enabled
	 */
	LoginResponse login(LoginRequest request, String tokenIssuer);
	
	/**
	 * Bypass password check and just create a login response for the user, if the user has 2FA enabled TwoFactorAuthRequiredException is thrown.
	 * 
	 * @param principalId
	 * @param tokenIssuer
	 * @return
	 * @throws TwoFactorAuthRequiredException If the user has 2FA enabled
	 */
	LoginResponse loginWithNoPasswordCheck(long principalId, String tokenIssuer);
	
	/**
	 * Performs the login through 2FA, the request can be built out of the TwoFactorAuthRequiredException and an otp code
	 * 
	 * @param request
	 * @param issuer
	 * @return
	 */
	LoginResponse loginWith2Fa(TwoFactorAuthLoginRequest request, String issuer);

	/**
	 * Sends a notification to reset 2fa for the user specified in the request
	 * @param request
	 */
	void send2FaResetNotification(TwoFactorAuthResetRequest request);

	/**
	 * Uses the given reset request to disable 2fa for the user specified in the request
	 * @param request
	 */
	void disable2FaWithToken(TwoFactorAuthDisableRequest request);

}
