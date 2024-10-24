package org.sagebionetworks.client;

import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;

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

	@Deprecated
	/**
	 * Authenticate the Synapse client with an existing session token
	 */
	public void setSessionToken(String sessionToken);
	
	/**
	 * Set a uname and password as a Basic Authorization header.
	 * This should be used exclusively of the Synapse session token
	 * or any other authorization scheme.
	 * @param uname
	 * @param password
	 */
	public void setBasicAuthorizationCredentials(String uname, String password);
	
	/**
	 * Remove the Authorization Header
	 */
	public void removeAuthorizationHeader();
	
	/**
	 * Set a bearer authorization token.
	 * This should be used exclusively of the Synapse session token
	 * or any other authorization scheme.
	 * @param bearerToken
	 */
	public void setBearerAuthorizationToken(String bearerToken);

	@Deprecated
	/**
	 * Get the current session token used by this client.
	 * 
	 * @return the session token
	 */
	public String getCurrentSessionToken();

	/**
	 * Get the current access token used by this client.
	 * 
	 * @return the access token
	 */
	public String getAccessToken();

	/**
	 * Get the endpoint of the repository service
	 */
	public String getRepoEndpoint();

	/**
	 * The repository endpoint includes the host and version. For example:
	 * "https://repo-prod.prod.sagebase.org/repo/v1"
	 */
	public void setRepositoryEndpoint(String repoEndpoint);

	/**
	 * The authorization endpoint includes the host and version. For example:
	 * "https://repo-prod.prod.sagebase.org/auth/v1"
	 */
	public void setAuthEndpoint(String authEndpoint);

	/**
	 * Get the endpoint of the authorization service
	 */
	public String getAuthEndpoint();

	/**
	 * The file endpoint includes the host and version. For example:
	 * "https://repo-prod.prod.sagebase.org/file/v1"
	 */
	public void setFileEndpoint(String fileEndpoint);

	/**
	 * Get the endpoint of the file service
	 */
	public String getFileEndpoint();

	/**
	 * The drs endpoint includes the host and version. For example:
	 * " https://repo-prod.prod.sagebase.org/ga4gh/drs/v1"
	 */
	 void setDrsEndpoint(String drsEndpoint);

	/**
	 * Get the endpoint of the drs service
	 */
	 String getDrsEndpoint();

	public String getUserName();

	public void setUsername(String userName);

	public String getApiKey();

	public void setApiKey(String apiKey);
	
	/**
	 * Sets the ip address of the user that this client is performing actions for.
	 * @param ipAddress
	 */
	public void setUserIpAddress(String ipAddress);

	@Deprecated
	/**
	 * Log into Synapse
	 * 
	 * @param request
	 * @return a session token for subsequent requests
	 * @throws SynapseException
	 */
	LoginResponse login(LoginRequest request) throws SynapseException;
	
	/**
	 * Get the date/time when the requesting user was last authenticated.
	 * @return
	 * @throws SynapseException
	 */
	AuthenticatedOn getAuthenticatedOn() throws SynapseException;

	/**
	 * Log into Synapse
	 * 
	 * @param request
	 * @return an access token for subsequent requests
	 * @throws SynapseException
	 */
	LoginResponse loginForAccessToken(LoginRequest request) throws SynapseException;

	@Deprecated
	/**
	 * Log out of Synapse
	 */
	public void deleteSessionTokenHeader() throws SynapseException;

	public void logoutForAccessToken() throws SynapseException;
	
	/**
	 * Invalidate all access tokens
	 */
	void logoutAllForAccessToken() throws SynapseException;

	public void invalidateApiKey() throws SynapseException;

	/**
	 * Set the sessionId, which is used to identify a series of requests made by this client to the current repoEndpoint
	 * @param sessionId
	 */
	public void setSessionId(String sessionId);

	/**
	 * Get the sessionId, which is used to identify a series of requests made by this client to the current repoEndpoint
	 */
	public String getSessionId();
}
