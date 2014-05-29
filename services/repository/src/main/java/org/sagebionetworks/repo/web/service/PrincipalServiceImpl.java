package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.principal.PrincipalManager;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AddEmailInfo;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the PrincipalService.
 * 
 * @author John
 *
 */
public class PrincipalServiceImpl implements PrincipalService {
	@Autowired
	private UserManager userManager;
	
	@Autowired
	PrincipalManager principalManager;

	@Override
	public AliasCheckResponse checkAlias(AliasCheckRequest check) {
		// First is it valid?
		boolean isValid = principalManager.isAliasValid(check.getAlias(), check.getType());
		boolean isAvailable;
		if(isValid){
			// Check valid only.
			isAvailable = principalManager.isAliasAvailable(check.getAlias());
		}else{
			// Only valid aliases are available
			isAvailable = false;
		}
		AliasCheckResponse response = new AliasCheckResponse();
		response.setAvailable(isAvailable);
		response.setValid(isValid);
		return response;
	}
	
	/**
	 * Send an email validation message as a precursor to creating a new user account.
	 * 
	 * @param user the info for the new user
	 * @param portalEndoint the GUI endpoint (is the basis for the link in the email message)
	 * @param domain Synapse or Bridge
	 */
	public void newAccountEmailValidation(NewUser user, String portalEndoint, DomainType domain) {
		principalManager.newAccountEmailValidation(user, portalEndoint, domain);
	}
	
	/**
	 * Create a new account, following email validation
	 * @param accountSetupInfo
	 * @param domain
	 * @return session
	 * @throws NotFoundException 
	 */
	public Session createNewAccount(AccountSetupInfo accountSetupInfo, DomainType domain) throws NotFoundException {
		return principalManager.createNewAccount(accountSetupInfo, domain);
	}
	
	/**
	 * Send an email validation as a precursor to adding a new email address to an existing account.
	 * @param userInfo the authenticated user making the request
	 * @param email the email which is claimed by the user
	 * @param portalEndoint the GUI endpoint (is the basis for the link in the email message)
	 * @param domain Synapse or Bridge
	 * @throws NotFoundException 
	 */
	public void additionalEmailValidation(Long userId, Username email, String portalEndoint, DomainType domain) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		principalManager.additionalEmailValidation(userInfo, email, portalEndoint, domain);
	}
	
	/**
	 * Add a new email address to an existing account.
	 * 
	 * @param userInfo
	 * @param addEmailInfo
	 * @param setAsNotificationEmail
	 * @throws NotFoundException
	 */
	public void addEmail(Long userId, AddEmailInfo addEmailInfo, Boolean setAsNotificationEmail) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		principalManager.addEmail(userInfo, addEmailInfo, setAsNotificationEmail);
	}
	
	/**
	 * Remove an email address from an existing account.
	 * 
	 * @param userInfo
	 * @param email
	 * @throws NotFoundException
	 */
	public void removeEmail(Long userId, Username email) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		principalManager.removeEmail(userInfo, email);
	}
	
	/**
	 * Set the email address used for notification.  The address must already be
	 * registered as an alias for the given user.
	 * 
	 * @param userId
	 * @param email
	 */
	public void setNotificationEmail(Long userId, String email) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		principalManager.setNotificationEmail(userInfo, email);
	}

	/**
	 * Get the email address used for notification.
	 * 
	 * @param userId
	 */
	public Username getNotificationEmail(Long userId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return principalManager.getNotificationEmail(userInfo);
	}

}
