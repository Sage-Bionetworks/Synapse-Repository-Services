package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.principal.PrincipalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.Username;
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
