package org.sagebionetworks.repo.manager.principal;

import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AddEmailInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.web.NotFoundException;

public interface PrincipalManager {

	/**
	 * Is the passed alias available?
	 * 
	 * @param alias
	 * @param type
	 * @return
	 */
	boolean isAliasAvailable(String alias);

	/**
	 * Is the passed alias valid for the passed type?
	 * 
	 * @param alias
	 * @param type
	 * @return
	 */
	boolean isAliasValid(String alias, AliasType type);
	
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
	 * @param userInfo the authenticated user making the request
	 * @param email the email which is claimed by the user
	 * @param portalEndpoint the GUI endpoint (is the basis for the link in the email message)
	 * @param domain Synapse
	 * @throws NotFoundException
	 */
	void additionalEmailValidation(UserInfo userInfo, Username email, String portalEndpoint, DomainType domain) throws NotFoundException;
	
	/**
	 * Add a new email address to an existing account.
	 * 
	 * @param userInfo
	 * @param addEmailInfo
	 * @param setAsNotificationEmail
	 * @throws NotFoundException
	 */
	void addEmail(UserInfo userInfo, AddEmailInfo addEmailInfo, Boolean setAsNotificationEmail) throws NotFoundException;
	
	/**
	 * Remove an email address from an existing account.
	 * 
	 * @param userInfo
	 * @param email
	 * @throws NotFoundException
	 */
	void removeEmail(UserInfo userInfo, String email) throws NotFoundException;
	
	/**
	 * Set the email which is used for notification.
	 * 
	 * @param principalId
	 * @param email
	 * @throws NotFoundException 
	 */
	void setNotificationEmail(UserInfo userInfo, String email) throws NotFoundException;

	/**
	 * Get the email which is used for notification.
	 * @param userInfo
	 * @return
	 * @throws NotFoundException
	 */
	Username getNotificationEmail(UserInfo userInfo) throws NotFoundException;

	/**
	 * Get the principalId for the given alias and alias type
	 * 
	 * @param alias
	 * @return
	 */
	PrincipalAliasResponse lookupPrincipalId(PrincipalAliasRequest alias);
}
