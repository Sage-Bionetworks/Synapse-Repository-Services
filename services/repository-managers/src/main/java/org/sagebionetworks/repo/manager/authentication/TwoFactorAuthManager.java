package org.sagebionetworks.repo.manager.authentication;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthRecoveryCodes;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthResetToken;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthTokenContext;

public interface TwoFactorAuthManager {

	/**
	 * Generates a new totp (inactive) secret for the user. This method can be used even if the user has 2FA already enabled. The secret is stored encrypted.
	 * 
	 * @param user
	 * @return A new totp (inactive) secret for the user 
	 */
	TotpSecret init2Fa(UserInfo user);

	/**
	 * Enables 2FA for the user using the secret and code in the request. This method will reset the user 2FA if already enabled.
	 * 
	 * @param user
	 * @param request
	 */
	void enable2Fa(UserInfo user, TotpSecretActivationRequest request);
	
	/**
	 * @param user
	 * @return The current status of 2FA for the user
	 */
	TwoFactorAuthStatus get2FaStatus(UserInfo user);
	
	/**
	 * Disables 2FA for the user
	 * @param user
	 */
	void disable2Fa(UserInfo user);
	
	/**
	 * Validates the given totp code for the user
	 * 
	 * @param user
	 * @param totpCode
	 * @return True if the given totp code was valid for the given user
	 */
	boolean validate2FaTotpCode(UserInfo user, String totpCode);
	
	/**
	 * 
	 * @param user
	 * @param context The context within which the token is generated
	 * @return Base64 encoded token that can be used to perform operations with 2FA, restricted to the given set of OTP code types
	 */
	String generate2FaToken(UserInfo user, TwoFactorAuthTokenContext context);
	
	/**
	 * @param user
	 * @param context The context against which the token is validated, must match the context that was used when the token was generated
	 * @param encodedToken Base64 encoded token
	 * @return True if the given token is a valid 2FA token for the user and otp type
	 */
	boolean validate2FaToken(UserInfo user, TwoFactorAuthTokenContext context, String encodedToken);
	
	/**
	 * Generates a new (replaces old ones) set of recovery codes for the given user if 2FA is enabled. 
	 * The codes are one-time use and can be used when authenticating in place of a TOTP code
	 * 
	 * @param user
	 * @return
	 */
	TwoFactorAuthRecoveryCodes generate2FaRecoveryCodes(UserInfo user);
	
	/**
	 * Validates the given recovery code, once the code is validated it will be unusable
	 * 
	 * @param user
	 * @param recoveryCode
	 * @return True if the code is valid, false otherwise
	 */
	boolean validate2FaRecoveryCode(UserInfo user, String recoveryCode);

	/**
	 * Send a notification to the user with a link to reset their 2fa
	 * 
	 * @param user
	 * @param twoFaResetEndpoint
	 */
	void send2FaResetNotification(UserInfo user, String twoFaResetEndpoint);

	/**
	 * Validates the given 2fa reset token
	 * 
	 * @param user
	 * @param token
	 * @return
	 */
	boolean validate2FaResetToken(UserInfo user, TwoFactorAuthResetToken token);
}
