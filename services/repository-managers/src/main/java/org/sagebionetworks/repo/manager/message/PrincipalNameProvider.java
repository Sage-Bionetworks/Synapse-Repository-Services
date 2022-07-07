package org.sagebionetworks.repo.manager.message;

public interface PrincipalNameProvider {

	
	/**
	 * Get either the username or team name for the given principal id.
	 * @param userName
	 * @return
	 */
	String getPrincipalName(Long principalId);
}
