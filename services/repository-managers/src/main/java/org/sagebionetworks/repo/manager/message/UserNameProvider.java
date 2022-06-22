package org.sagebionetworks.repo.manager.message;

public interface UserNameProvider {

	/**
	 * Get the display name for the given principal ID.
	 * @param principalId
	 * @return
	 */
	String getPrincipalDisplayName(Long principalId);
	
	/**
	 * Get either the username or team name for the given principal id.
	 * @param userName
	 * @return
	 */
	String getPrincipaleName(Long principalId);
}
