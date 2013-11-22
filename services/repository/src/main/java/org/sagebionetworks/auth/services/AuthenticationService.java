package org.sagebionetworks.auth.services;

import org.openid4java.message.ParameterList;
import org.sagebionetworks.repo.model.OriginatingClient;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.ChangePasswordRequest;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for the handling authentication
 */
public interface AuthenticationService {
	
	/**
	 * Authenticates a user/password combination, returning a session token if valid
	 * @throws UnauthorizedException If the credentials are incorrect
	 */
	public Session authenticate(LoginCredentials credential) throws NotFoundException;
	
	/**
	 * Revalidates a session token and checks if the user has accepted the terms of use
	 * @return The principalId of the user holding the token
	 * @throws UnauthorizedException If the token has expired or is otherwise not valid
	 * @throws TermsOfUseException If the user has not accepted the terms of use
	 */
	public String revalidate(String sessionToken);
	
	/**
	 * Revalidates a session token
	 * See {@link #revalidate(String)}
	 * @param checkToU Should the check fail if the user has not accepted the terms of use?
	 */
	public String revalidate(String sessionToken, boolean checkToU);
	
	/**
	 * Invalidates a session token
	 */
	public void invalidateSessionToken(String sessionToken);
	
	/**
	 * Initializes a new user into the system
	 * @throws UnauthorizedException If a user with the supplied email already exists 
	 */
	public void createUser(NewUser user, OriginatingClient originClient);
	
	/**
	 * Sends a password-reset email to the user
	 * Note: Email is not actually sent in development stacks.  Instead a log appears when email would have been sent
	 */
	public void sendPasswordEmail(String username, OriginatingClient originClient)
			 throws NotFoundException;
	
	/**
	 * Changes the password of the user
	 */
	public void changePassword(ChangePasswordRequest request);
	
	/**
	 * Identifies a user via session token and signs that user's terms of use
	 */
	public void signTermsOfUse(Session session) throws NotFoundException;
	
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
	 * Returns the username of the user
	 */
	public String getUsername(String principalId)
			throws NotFoundException;
	
	/**
	 * Uses the pre-validated OpenID information to fetch a session token
	 * Will create a user if necessary 
	 */
	public Session authenticateViaOpenID(ParameterList parameters) throws NotFoundException;
}
