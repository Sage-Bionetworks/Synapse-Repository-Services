package org.sagebionetworks.repo.manager.grid;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;

public interface GridAuthorizationManager {

	/**
	 * Validate the user has access to this grid session.
	 * 
	 * @param user
	 * @param sessionId
	 * @return
	 */
	AuthorizationStatus hasGridSessionAccess(UserInfo user, String sessionId);

	/**
	 * Get the UserInfo that can be used for row level filtering of table/view query
	 * results from the grid's owner information.
	 * 
	 * @param user
	 * @param sessionId
	 * @return
	 */
	UserInfo getRowLevelFilterUserInfo(UserInfo user, String sessionId);
	
	/**
	 * 
	 * @param user
	 * @param ownerString
	 */
	Long validateGridOwner(UserInfo user, String ownerString);
	
}
