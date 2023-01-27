package org.sagebionetworks.repo.manager.authentication;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthOtpType;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;

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
	 * @param user
	 * @param otpCode
	 * @return True if the given otp code is valid for the given user
	 */
	boolean is2FaCodeValid(UserInfo user, TwoFactorAuthOtpType otpType, String otpCode);
	
	/**
	 * @param user
	 * @return Base64 encoded token that can be used to perform authentication with 2FA
	 */
	String generate2FaLoginToken(UserInfo user);
	
	/**
	 * @param user
	 * @param encodedToken Base64 encoded token
	 * @return True if the given token is a valid 2FA login token for the user
	 */
	boolean is2FaLoginTokenValid(UserInfo user, String encodedToken);
	
}
