package org.sagebionetworks.auth.services;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.RegistrationInfo;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for the handling authentication
 */
public interface AuthenticationService {
	
	public static enum PW_MODE {
		SET_PW,
		RESET_PW,
		SET_API_PW
	}
	/**
	 * Authenticates a user/password combination, returning a session token if valid
	 *   
	 * @param caller Optional parameter.  Only used to determine when the portal is making the call.
	 *   When the portal is the caller, password and ToU checking is relaxed.
	 *   This allows independently validated users to get a session token without supplying complete credentials. 
	 * @throws UnauthorizedException If the credentials are incorrect
	 */
	public Session authenticate(NewUser credential, String caller)
			throws NotFoundException, UnauthorizedException;
	
	/**
	 * Revalidates a session token and checks whether the user has accepted the terms of use
	 * @return The principalId of the user holding the token
	 * @throws UnauthorizedException If the token has expired or is otherwise not valid
	 */
	public String revalidate(String sessionToken) throws NotFoundException, UnauthorizedException;
	
	/**
	 * Invalidates a session token
	 */
	public void invalidateSessionToken(String sessionToken);

	/**
	 * Returns whether the given user has accepted the most recent terms of use
	 */
	public boolean hasUserAcceptedTermsOfUse(String id)
			throws NotFoundException;
	
	/**
	 * Initializes a new user into the system
	 * @throws UnauthorizedException If a user with the supplied email already exists 
	 */
	public void createUser(NewUser user)
			throws UnauthorizedException;
	
	/**
	 * Returns information on the user
	 */
	public UserInfo getUserInfo(String username)
			throws NotFoundException;
	
	/**
	 * Changes the password of the user
	 */
	public void changePassword(String username, String newPassword)
			throws NotFoundException, NoSuchAlgorithmException, InvalidKeySpecException;
	
	/**
	 * Changes the email of a user to another email
	 */
	public void updateEmail(String oldEmail, RegistrationInfo registrationInfo)
			throws NotFoundException;
	
	/**
	 * Gets the current secret key of the user
	 */
	public String getSecretKey(String username)
			throws NotFoundException;
	
	/** 
	 * Invalidates the user's secret key
	 */
	public void deleteSecretKey(String username)
			throws NotFoundException;
	
	/**
	 * Gets a regular Synapse session token for a user (i.e. 'logs them in')
	 * Should only be called after authenticating the user.
	 */
	public String getSessionTokenFromUserName(String userName) throws NotFoundException;

	/**
	 * Returns whether the given user is an administrator
	 */
	public boolean isAdmin(String username)
			throws NotFoundException;
	
	/**
	 * Returns the username of the user
	 */
	public String getUsername(String principalId)
			throws NotFoundException;
	
	
	/**
	 * Sends a password-related email to the user
	 * This method assumes that the required authentication has passed for the user
	 * Note: Should only send emails in Production stacks (instead log where emails would have been sent)
	 */
	public void sendUserPasswordEmail(String username, PW_MODE mode)
			 throws NotFoundException;
}
