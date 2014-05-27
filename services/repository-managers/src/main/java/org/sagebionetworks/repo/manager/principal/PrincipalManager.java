package org.sagebionetworks.repo.manager.principal;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.principal.AliasType;
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
}
