/**
 * 
 */
package org.sagebionetworks.repo.model;

/**
 * @author deflaux
 * 
 */
public class AuthorizationConstants {

	/**
	 * These are default groups that are guaranteed to be there.
	 */
	public enum DEFAULT_GROUPS {
		AUTHENTICATED_USERS,
		PUBLIC,
		BOOTSTRAP_USER_GROUP;
		
		/**
		 * Does the name match a default group?
		 * @param name
		 * @return
		 */
		public static boolean isDefaultGroup(String name){
			for(DEFAULT_GROUPS dg: DEFAULT_GROUPS.values()){
				if(dg.name().equals(name)) return true;
			}
			return false;
		}
	}
	
	/**
	 * User Group to have ownership of bootstrapped entities
	 * 
	 */
	public static final String BOOTSTRAP_USER_GROUP_NAME = DEFAULT_GROUPS.BOOTSTRAP_USER_GROUP.name();
	
	/**
	 * BOOTSTRAP group must always have the same ID to allow stack migration
	 * 
	 */
//	public static final String BOOTSTRAP_USER_GROUP_ID = "0";
	
	/**
	 * A scheme that describes how an ACL should be applied to an entity.
	 */
	public enum ACL_SCHEME{
		GRANT_CREATOR_ALL,
		INHERIT_FROM_PARENT,
	}
	
	/**
	 * The group name for a system defined group which allows access to its
	 * resources to all (including anonymous users)
	 */
//	public static final String PUBLIC_GROUP_NAME = "Identified Users";
	@Deprecated
	public static final String PUBLIC_GROUP_NAME = DEFAULT_GROUPS.PUBLIC.name();
	
	/**
	 * The group name for those users that have all kinds of access to all resources.
	 */
	public static final String ADMIN_GROUP_NAME = "Administrators";
	
	/**
	 * 
	 */
	public static final String ACCESS_AND_COMPLIANCE_TEAM_NAME = "Synpase ACT Team";
	
	/**
	 * The reserved userId for an anonymous user.
	 */
	public static final String ANONYMOUS_USER_ID = "anonymous@sagebase.org";
	public static final String ANONYMOUS_USER_DISPLAY_NAME = "Anonymous";
	
	/**
	 * Per http://sagebionetworks.jira.com/browse/PLFM-192
	 * authenticated requests made with an API key have the following
	 * three header fields
	 */
	public static final String USER_ID_HEADER = "userId";
	public static final String SIGNATURE_TIMESTAMP = "signatureTimestamp";
	public static final String SIGNATURE = "signature";	

	public static final String CROWD_SECRET_KEY_ATTRIBUTE = "AuthenticationSecretKey";
	
	public static final String TERMS_OF_USE_URI = "/termsOfUse";
	public static final String TERMS_OF_USE_AGREEMENT_URI = "/termsOfUseAgreement";
	
	public static final String ACCEPTS_TERMS_OF_USE_ATTRIBUTE = "org.sagebionetworks.termsOfUseAgreement.AcceptsTermsOfUse";


	/**
	 * Request parameter for the authenticated user id or anonymous. Note that
	 * callers of the service do not actually use this parameter. Instead they
	 * use a token parameter which is then converted to a user id by a request
	 * pre-processing filter.
	 */
	public static final String USER_ID_PARAM = "userId";
	
	
	/**
	 * A reserved parameter name for passing in a user id (not necessarily the name of the requestor,
	 * which is given by USER_ID_PARAM)
	 */
	public static final String USER_NAME_PARAM = "userName";
	/**
	 * The header for the session token
	 */
	public static final String SESSION_TOKEN_PARAM = "sessionToken";
	
	/**
	 * For special cases where the session token is added as a cookie, this is the name of the cookie we look for.
	 */
	public static final String SESSION_TOKEN_COOKIE_NAME = "org.sagbionetworks.security.user.login.token";

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
	
	/**
	 * Request parameter for an optimistic concurrency control (OCC) eTag.
	 */
	public static final String ETAG_PARAM = "etag";
	
}
