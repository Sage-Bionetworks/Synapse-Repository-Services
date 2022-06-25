package org.sagebionetworks.repo.manager.message;

public interface UserNameProvider {

	
	/**
	 * Get either the username or team name for the given principal id.
	 * @param userName
	 * @return
	 */
	String getPrincipalName(Long principalId);
}
