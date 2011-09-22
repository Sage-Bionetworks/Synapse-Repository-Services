package org.sagebionetworks.authutil;

@Deprecated // use org.sagebionetworks.repo.model.AuthorizationConstants
public class AuthUtilConstants {
	
	
	/**
	 * Request parameter for the authenticated user id or anonymous. Note that
	 * callers of the service do not actually use this parameter. Instead they
	 * use a token parameter which is then converted to a user id by a request
	 * pre-processing filter.
	 */
	public static final String USER_ID_PARAM = "userId";
	
	/**
	 * The header for the session token
	 */
	public static final String SESSION_TOKEN_PARAM = "sessionToken";

	/**
	 * This is the name of the attribute in Crowd for the creation date for a User
	 */
	public static final String CREATION_DATE_FIELD = "creationDate";
	
	/**
	 * Used to set a mock crowd.
	 */
	public static final String USER_DAO_INTEGRATION_TEST_SWITCH = "org.sagebionetworks.mockCrowdDAOClass";
	
	/**
	 * Accept all SSL certificates, when acting as an HTTPS client.
	 */
	public static final String ACCEPT_ALL_CERTS = "ACCEPT_ALL_CERTS";
	
	/**
	 * The name of a system property which indicates the interval at which that the Authentication filter
	 * and the UserManager should invalidate the cached the auth' info, in units of milliseconds.
	 * 
	 * A value of zero means that caching is not used.
	 */
	public static final String AUTH_CACHE_TIMEOUT_MILLIS = "org.sagebionetworks.authCacheTimeoutMillis";
	
	/**
	 * The default auth cache invalidation interval
	 */
	public static final long AUTH_CACHE_TIMEOUT_DEFAULT = 60000L;
	
}
