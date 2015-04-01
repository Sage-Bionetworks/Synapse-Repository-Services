package org.sagebionetworks.client;


/**
 * Low-level Java Client API for REST APIs
 */
public class BaseClientImpl implements BaseClient {

	private String userAgent;

	private final SharedClientConnection sharedClientConnection;

	/**
	 * Default client provider.
	 * 
	 * @param clientProvider
	 */

	protected BaseClientImpl(String userAgent, SharedClientConnection sharedClientConnection) {
		if (sharedClientConnection == null)
			throw new IllegalArgumentException("SharedClientConnection cannot be null");

		this.userAgent = userAgent;
		this.sharedClientConnection = sharedClientConnection;
	}

	/**
	 * get the shared client connection for reuse in a synapse client
	 * 
	 * @return
	 */
	@Override
	public SharedClientConnection getSharedClientConnection() {
		return sharedClientConnection;
	}

	/**
	 * Each request includes the 'User-Agent' header. This is set to:
	 * 'User-Agent':'Synapse-Java-Client/<version_number>' Addition User-Agent information can be appended to this
	 * string by calling this method.
	 * 
	 * @param toAppend
	 */
	@Override
	public void appendUserAgent(String toAppend) {
		// Only append if it is not already there
		if (this.userAgent.indexOf(toAppend) < 0) {
			this.userAgent = this.userAgent + "  " + toAppend;
		}
	}

	protected String getUserAgent() {
		return userAgent;
	}

	/**
	 * Authenticate the synapse client with an existing session token
	 * 
	 */
	@Override
	public void setSessionToken(String sessionToken) {
		this.sharedClientConnection.setSessionToken(sessionToken);
	}

	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	@Override
	public String getCurrentSessionToken() {
		return this.sharedClientConnection.getCurrentSessionToken();
	}
}
