package org.sagebionetworks.web.server.servlet;

public interface TokenProvider {
	
	/**
	 * Get the user's Synapse session token.
	 * @return
	 */
	public String getSessionToken();

}
