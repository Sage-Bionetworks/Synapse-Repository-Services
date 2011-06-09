package org.sagebionetworks.authutil;

public class AuthUtilConstants {
	
	// TODO delete this, as it repeats the same value defined in AuthorizationConstants
	// (also have to review the package dependencies)
	/**
	 * The reserved userId for an anonymous user.
	 */
	public static final String ANONYMOUS_USER_ID = "anonymous";
	
	
//	/**
//	 * The reserved userId for an administrator.  Note:  Other users in the admin group
//	 * would have privileges equal to this users'.  This is the user that the authentication
//	 * service uses to connect to the repository service.
//	 */
//	public static final String ADMIN_USER_ID = "admin";
	
	/**
	 * Request parameter for the authenticated user id or anonymous. Note that
	 * callers of the service do not actually use this parameter. Instead they
	 * use a token parameter which is then converted to a user id by a request
	 * pre-processing filter.
	 */
	public static final String USER_ID_PARAM = "userId";

	/**
	 * application id and password for Crowd REST API
	 */
	public static final String CLIENT = "platform";
	public static final String CLIENT_KEY = "platform-pw";

	/**
	 * Group name for users added to Crowd.  Crowd REST API only allows retrieving users by group, 
	 * therefore to retrieve the users in Crowd they must be added to some group.
	 */
	public static final String PLATFORM_GROUP = "platform";
	
	/**
	 * This is the name of the attribute in Crowd for the creation date for a User
	 */
	public static final String CREATION_DATE_FIELD = "creationDate";

	/** 
	 * The date format for the above
	 * 
	 */
	public static final String DATE_FORMAT = "yyyy-mm-dd";

	
	/**
	 * Used to set a mock crowd.
	 */
	public static final String USER_DAO_INTEGRATION_TEST_SWITCH = "org.sagebionetworks.mockCrowdDAOClass";
	
	
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
