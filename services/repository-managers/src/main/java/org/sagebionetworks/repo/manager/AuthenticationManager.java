package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.ChangePasswordInterface;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;


public interface AuthenticationManager {

	
	/**
	 * Looks for the user holding the given session token
	 * @throws UnauthorizedException If the token has expired
	 */
	public Long getPrincipalId(String sessionToken);
	
	/**
	 * Looks for the given session token
	 * Also revalidates the session token if valid
	 * 
	 * @param checkToU Should an exception be thrown if the terms of use haven't been signed?
	 * @return The principal ID of the holder
	 * @throws UnauthorizedException If the token is not valid
	 * @throws TermsOfUseException If the user has not signed the terms of use
	 */
	public Long checkSessionToken(String sessionToken, boolean checkToU) throws NotFoundException;
	
	/**
	 * Deletes the given session token, thereby invalidating it
	 */
	public void invalidateSessionToken(String sessionToken);
	
	/**
	 * Set a user's password without any authorization checks
	 */
	public void setPassword(Long principalId, String password);

	/**
	 * Change a user's password after checking the validity of the request by checking the user's old password
	 * @return id of the user whose password has been changed
	 */
	public long changePassword(ChangePasswordInterface changePasswordWithOldPassword);

	/**
	 * Gets the user's secret key
	 */
	public String getSecretKey(Long principalId) throws NotFoundException;
	
	/**
	 * Replaces the user's secret key with a new one
	 */
	public void changeSecretKey(Long principalId);
	
	/**
	 * Returns the user's session token
	 * If the user's token is invalid or expired, a new one is created and returned
	 */
	public Session getSessionToken(long principalId) throws NotFoundException;

	/**
	 * Creates a token tha can be used to reset a user's password
	 */
	public PasswordResetSignedToken createPasswordResetToken(long principalId) throws NotFoundException;
	
	/**
	 * Returns whether the user has accepted the terms of use
	 */
	public boolean hasUserAcceptedTermsOfUse(Long id) throws NotFoundException;

	/**
	 * Sets whether the user has accepted or rejected the terms of use
	 */
	public void setTermsOfUseAcceptance(Long principalId, Boolean acceptance);


	/**
	 * Log user in using information form the LoginRequest
	 * @param request
	 * @return
	 */
	public LoginResponse login(LoginRequest request);

	/**
	 * Bypass password check and just create a login response for the user.
	 * @param principalId
	 * @return
	 */
	public LoginResponse loginWithNoPasswordCheck(long principalId);
}
