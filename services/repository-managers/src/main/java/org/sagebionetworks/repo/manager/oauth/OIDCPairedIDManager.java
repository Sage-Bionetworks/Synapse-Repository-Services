package org.sagebionetworks.repo.manager.oauth;

public interface OIDCPairedIDManager {
	/**
	 * Given a Synapse userId and an OAuthClient ID, create the 
	 * OpenIDConnect Pseudonymous Paired Identifier.
	 * @param userId
	 * @param clientId
	 * @return
	 */
	public String getPPIDFromUserId(String userId, String clientId);

	/**
	 * Given a Pseudonymous Paired Identifier and an OAuth client ID,
	 * return the Synapse User ID.
	 * @param ppid
	 * @param clientId
	 * @return
	 */
	public String getUserIdFromPPID(String ppid, String clientId);

}
