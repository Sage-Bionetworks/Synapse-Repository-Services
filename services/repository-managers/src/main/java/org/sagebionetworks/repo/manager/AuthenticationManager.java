package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AccessTokenResponse;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.ClearProfileInterface;
import org.sagebionetworks.repo.model.auth.ClearProfileRequest;
import org.sagebionetworks.repo.model.auth.ClearProfileWithTwoFactorAuthToken;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthDisableRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthLoginRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthResetRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TwoFactorAuthEnabledRequiredException;
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
	 * Validates a step of the process to clear the profile of the given user. The process requires the user to have two factor
	 * authentication enabled:
	 * <ul>
	 * <li>Step 1, a {@link ClearProfileRequest} with the user's password: this method always throws. A
	 * {@link TwoFactorAuthRequiredException} that includes a token if the password is correct and 2FA is enabled, or a
	 * {@link TwoFactorAuthEnabledRequiredException} if 2FA is not enabled.</li>
	 * <li>Step 2, a {@link ClearProfileWithTwoFactorAuthToken} with the 2FA token and a one-time code: this method returns
	 * normally only if both are valid.</li>
	 * </ul>
	 * 
	 * @param user The authenticated user that is clearing their profile
	 * @param request Step 1: the user's password. Step 2: a 2FA token with a one-time code
	 */
	void validateClearProfile(UserInfo user, ClearProfileInterface request);

	/**
	 * Creates a token tha can be used to reset a user's password
	 */
	PasswordResetSignedToken createPasswordResetToken(long principalId) throws NotFoundException;
	
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
	
	
	/**return an access token for the anonymous user in the given realm
	 * @param realmId
	 * @param issuer
	 * @return
	 */
	AccessTokenResponse getAnonymousAccessToken(String realmId, String issuer);
	
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
	 * Bypass password and 2fa check and just create a login response for the user
	 * 
	 * @param principalId
	 * @return
	 */
	LoginResponse loginWithNoPasswordOrTwoFaCheck(UserInfo userInfo, String tokenIssuer);
	
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
