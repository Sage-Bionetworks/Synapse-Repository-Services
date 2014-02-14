package org.sagebionetworks.repo.model;

/**
 * @author deflaux
 */
public class AuthorizationConstants {

	/**
	 * These are default users and groups that are guaranteed to be initialized by the system
	 * 
	 * The values reflect that of dao-beans.spb.xml  
	 */
	public enum BOOTSTRAP_PRINCIPAL {
		THE_ADMIN_USER(1L), 
		ADMINISTRATORS_GROUP(2L), 
		AUTHENTICATED_USERS_GROUP(273948L), 
		PUBLIC_GROUP(273949L), 
		ANONYMOUS_USER(273950L),
		ACCESS_AND_COMPLIANCE_GROUP(464532);

		private final long principalId;
		
		public Long getPrincipalId() {
			return this.principalId;
		}

		private BOOTSTRAP_PRINCIPAL(long principalId) {
			this.principalId = principalId;
		}
	}
	
	/**
	 * A scheme that describes how an ACL should be applied to an entity.
	 */
	public enum ACL_SCHEME{
		GRANT_CREATOR_ALL,
		INHERIT_FROM_PARENT,
	}
	
	/**
	 * Per http://sagebionetworks.jira.com/browse/PLFM-192
	 * authenticated requests made with an API key have the following
	 * three header fields
	 */
	public static final String USER_ID_HEADER = "userId";
	public static final String SIGNATURE_TIMESTAMP = "signatureTimestamp";
	public static final String SIGNATURE = "signature";	
	
	public static final String TERMS_OF_USE_URI = "/termsOfUse";


	/**
	 * Request parameter for a Team ID
	 */
	public static final String TEAM_ID_PARAM = "teamId";
	
	/**
	 * Request parameter for the authenticated user id or anonymous. Note that
	 * callers of the service do not actually use this parameter. Instead they
	 * use a token parameter which is then converted to a user id by a request
	 * pre-processing filter.
	 */
	public static final String USER_ID_PARAM = "userId";
	
	/**
	 * The name of the client make the REST call. For a few calls, behavior will 
	 * change depending on whether this is Bridge or a Synapse client (at the least, 
	 * email contents change).
	 */
	public static final String DOMAIN_PARAM = "originClient";
	
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
	 * Request parameter for an optimistic concurrency control (OCC) eTag.
	 */
	@Deprecated
	public static final String ETAG_PARAM = "etag";
	
}
