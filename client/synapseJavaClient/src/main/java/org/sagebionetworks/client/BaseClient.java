package org.sagebionetworks.client;


/**
 * Abstraction for Synapse.
 * 
 * @author jmhill
 * 
 */
public interface BaseClient {

	/**
	 * Each request includes the 'User-Agent' header. This is set to:
	 * 
	 * 'User-Agent':'*-Java-Client/<version_number>'
	 * 
	 * @param toAppend Addition User-Agent information can be appended to this string via this parameter
	 */
	public void appendUserAgent(String toAppend);

	/**
	 * Authenticate the Synapse client with an existing session token
	 */
	public void setSessionToken(String sessionToken);

	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	public String getCurrentSessionToken();

	/**
	 * get the shared client connection for reuse in another client
	 * 
	 * @return
	 */
	public SharedClientConnection getSharedClientConnection();
}
