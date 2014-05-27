package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
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

}
