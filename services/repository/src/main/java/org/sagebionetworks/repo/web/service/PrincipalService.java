package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AddEmailInfo;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for Principal lookup.
 * 
 * @author John
 *
 */
public interface PrincipalService {

	/**
	 * Check if an alias is available.
	 * @param check
	 * @return
	 */
	AliasCheckResponse checkAlias(AliasCheckRequest check);
	
	/**
	 * Send an email validation message as a precursor to creating a new user account.
	 * 
	 * @param user the info for the new user
	 * @param portalEndpoint the GUI endpoint (is the basis for the link in the email message)
	 * @param domain Synapse
	 */
	void newAccountEmailValidation(NewUser user, String portalEndpoint, DomainType domain);
	
	/**
	 * Create a new account, following email validation
	 * @param accountSetupInfo
	 * @param domain
	 * @return session
	 * @throws NotFoundException 
	 */
	Session createNewAccount(AccountSetupInfo accountSetupInfo, DomainType domain) throws NotFoundException;
	
	/**
	 * Send an email validation as a precursor to adding a new email address to an existing account.
	 * 
	 * @param userId the authenticated user making the request
	 * @param email the email which is claimed by the user
	 * @param portalEndpoint the GUI endpoint (is the basis for the link in the email message)
	 * @param domain Synapse
	 * @throws NotFoundException
	 */
	void additionalEmailValidation(Long userId, Username email, String portalEndpoint, DomainType domain) throws NotFoundException;
	
	/**
	 * Add a new email address to an existing account.
	 * 
	 * @param userId
	 * @param addEmailInfo
	 * @param setAsNotificationEmail
	 * @throws NotFoundException
	 */
	void addEmail(Long userId, AddEmailInfo addEmailInfo, Boolean setAsNotificationEmail) throws NotFoundException;
	
	/**
	 * Remove an email address from an existing account.
	 * 
	 * @param userId
	 * @param email
	 * @throws NotFoundException
	 */
	void removeEmail(Long userId, String email) throws NotFoundException;	
	
	/**
	 * Set the email address used for notification.  The address must already be
	 * registered as an alias for the given user.
	 * 
	 * @param userId
	 * @param email
	 */
	void setNotificationEmail(Long userId, String email) throws NotFoundException;

	/**
	 * Get the email address used for notification.  The address must already be
	 * registered as an alias for the given user.
	 * 
	 * @param userId
	 * @return the email address
	 */
	Username getNotificationEmail(Long userId) throws NotFoundException;

	/**
	 * Get the principal ID for a given alias and alias type
	 * 
	 * @param alias
	 * @return
	 */
	PrincipalAliasResponse getPrincipalAlias(PrincipalAliasRequest alias);

}
