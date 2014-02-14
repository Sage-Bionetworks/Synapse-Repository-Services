package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.web.NotFoundException;


public interface AuthenticationManager {

	/**
	 * Authenticates a user/password combination, returning a session token if valid
	 * @param password If null, password checking is skipped
	 */
	public Session authenticate(long principalId, String password, DomainType domain) throws NotFoundException;
	
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
	public Long checkSessionToken(String sessionToken, DomainType domain, boolean checkToU) throws NotFoundException;
	
	/**
	 * Deletes the given session token, thereby invalidating it
	 */
	public void invalidateSessionToken(String sessionToken);
	
	/**
	 * Changes a user's password
	 */
	public void changePassword(Long principalId, String password);
	
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
	public Session getSessionToken(long principalId, DomainType domain) throws NotFoundException;
	
	/**
	 * Returns whether the user has accepted the terms of use
	 */
	public boolean hasUserAcceptedTermsOfUse(Long id, DomainType domain) throws NotFoundException;

	/**
	 * Sets whether the user has accepted or rejected the terms of use
	 */
	public void setTermsOfUseAcceptance(Long principalId, DomainType domain, Boolean acceptance);
}
