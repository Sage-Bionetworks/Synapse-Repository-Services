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
		AUTHENTICATED_USERS_GROUP(273948L), // Group that includes all Synapse users, except Anonymous
		PUBLIC_GROUP(273949L), // Group that includes all users, including the Anonymous user
		ANONYMOUS_USER(273950L), // "Anonymous" is an individual that represents users that are not logged in.
		CERTIFIED_USERS(3L),
		ADMINISTRATORS_GROUP(2L),
		ACCESS_AND_COMPLIANCE_GROUP(464532L),
		TRUSTED_MESSAGE_SENDER_GROUP(4L),
		SYNAPSE_REPORT_GROUP(5L),
		DATA_ACCESS_NOTFICATIONS_SENDER(3412389L), // The synapse user used to send data access notifications
		SYNAPSE_TESTING_GROUP(6L); // Internal group that can be used to enable features on a subset of users

		private final long principalId;
		
		public Long getPrincipalId() {
			return this.principalId;
		}

		private BOOTSTRAP_PRINCIPAL(long principalId) {
			this.principalId = principalId;
		}
		
		/**
		 * Is the given ID a bootstrap principal ID?
		 * @param id
		 * @return
		 */
		public static boolean isBootstrapPrincipalId(long id) {
			for(BOOTSTRAP_PRINCIPAL principal: BOOTSTRAP_PRINCIPAL.values()) {
				if(principal.getPrincipalId().equals(id)) {
					return true;
				}
			}
			return false;
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
	 * The name of the client make the REST call. For a few calls, behavior will change depending on domain (at the
	 * least, email contents change).
	 */
	public static final String DOMAIN_PARAM = "domain";
	
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
	 * The web endpoint specified by the Portal for email validation
	 */
	public static final String PORTAL_ENDPOINT_PARAM = "portalEndpoint";
	
	/**
	 * A request parameter to the 'delete email address' service, specifying the email to delete
	 */
	public static final String EMAIL_PARAM = "email";
	
	/**
	 * A request parameter to the 'add email address' service, saying whether the new address should become the
	 * user's notifiation address.
	 */
	public static final String SET_AS_NOTIFICATION_EMAIL_PARM = "setAsNotificationEmail";
	
	/**
	 * Request parameter for an optimistic concurrency control (OCC) eTag.
	 */
	public static final String ETAG_PARAM = "etag";
	
	/**
	 * Request parameter giving the hash of the TeamSubmissionEligibility object which was
	 * referenced by the client when crafting the associated Submission.
	 */
	public static final String SUBMISSION_ELIGIBILITY_HASH_PARAM = "submissionEligibilityHash";
	
	/**
	 * A request parameter to the setUserCertification service, saying whether to set or clear certification.
	 */
	public static final String IS_CERTIFIED = "isCertified";
	
	/**
	 * A request parameter used in the AccessRequirementController to filter unmet access requirements on access type.
	 */
	public static final String ACCESS_TYPE_PARAM = "accessType";

	/**
	 * Response message for 500.
	 */
	public static final String REASON_SERVER_ERROR = "{\"reason\": \"Server Error. Error logged.\"}";
	
	/**
	 * A request parameter for specifying the portal endpoint for accepting a team membership invitation
	 */
	public static final String ACCEPT_INVITATION_ENDPOINT_PARAM = "acceptInvitationEndpoint";
	
	/**
	 * A request parameter for specifying the portal endpoint for accepting a team membership request
	 */
	public static final String ACCEPT_REQUEST_ENDPOINT_PARAM = "acceptRequestEndpoint";
	
	/**
	 * A request parameter for specifying the portal prefix for the Team URL.
	 * The team ID is appended to create the complete URL.
	 */
	public static final String TEAM_ENDPOINT_PARAM = "teamEndpoint";
	
	/**
	 * A request parameter for specifying the portal prefix for the Challenge Entity page URL.
	 * The entity ID is appended to create the complete URL.
	 */
	public static final String CHALLENGE_ENDPOINT_PARAM = "challengeEndpoint";
	
	/**
	 * A request parameter for specifying the portal endpoint for unsubscribing from email
	 */
	public static final String NOTIFICATION_UNSUBSCRIBE_ENDPOINT_PARAM = "notificationUnsubscribeEndpoint";

	/**
	 * Request parameter for specifying the portal endpoint for resetting user's password.
	 */
	public static final String PASSWORD_RESET_PARAM = "passwordResetEndpoint";

	/**
	 * Request parameter for the Docker authorization request.
	 * 
	 * This is the host name of the registry host making the request.
	 */
	public static final String DOCKER_SERVICE_PARAM = "service";
	
	/**
	 * Request parameter for the Docker authorization request.
	 * 
	 * 'scope' is the scope of the requested authorization.  It has three colon
	 * separated fields: 'type', 'path', and 'access types', where 'type' is
	 * 'repository', 'path' is the repo path within the registry and 'access types'
	 * is a comma delimited subset of 'push', 'pull'
	 */
	public static final String DOCKER_SCOPE_PARAM = "scope";
	
	
	/**
	 * The oauth 'client id' representing Synapse itself
	 */
	public static final String SYNAPSE_OAUTH_CLIENT_ID = "0";
	
	
	/**
	 * Request parameters for the Synapse OAuth server, defined by
	 * https://openid.net/specs/openid-connect-core-1_0.html#RefreshingAccessToken
	 */
	public static final String OAUTH2_SCOPE_PARAM = "scope";
	public static final String OAUTH2_CLAIMS_PARAM = "claims";
	public static final String OAUTH2_GRANT_TYPE_PARAM = "grant_type";
	public static final String OAUTH2_CODE_PARAM = "code";
	public static final String OAUTH2_REDIRECT_URI_PARAM = "redirect_uri";
	public static final String OAUTH2_REFRESH_TOKEN_PARAM = "refresh_token";
	public static final String OAUTH_VERIFIED_CLIENT_ID_HEADER = "verifiedOauthClientId";
	
	/**
	 * The Oauth access token is passed as the 'bearer' request header
	 */
	public static final String BEARER_TOKEN_HEADER = "Bearer ";
	
	/**
	 * Standard name for the HTTP Header containing credentials, bearer token, or other authentication/authorization information
	 */
	public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	
	/**
	 * Internal header name used when passing authorization from a Synapse HTTP Filter to a Spring Controller
	 */
	public static final String SYNAPSE_AUTHORIZATION_HEADER_NAME = "Synapse-Authorization";
	
	/**
	 * Internal header name used to identify a request that was authenticated for a service
	 */
	public static final String SYNAPSE_HEADER_SERVICE_NAME = "Synapse-Service-Name";
	
	public static final String BASIC_PREFIX = "Basic ";

	public static final String PROJECT_FILTER_PARAM = "filter";
	
	public static final String ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE = "You have not yet agreed to the Synapse Terms of Use.";
	public static final String ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE = "You lack %s access to the requested entity.";
	public static final String ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE = "You do not have %s permission for the requested entity, %s.";
	public static final String ERR_MSG_ONLY_CERTIFIED_USERS_MAY_CHANGE_NODE_SETTINGS = "Only certified users may change node settings.";
	public static final String ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS = "There are unmet access requirements that must be met to read content in the requested container.";
	public static final String ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION = "Anonymous users have only READ access permission.";
	public static final String ERR_MSG_CERTIFIED_USER_CONTENT = "Only certified users may create or update content in Synapse.";
	public static final String ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND = "The resource you are attempting to access cannot be found";
	public static final String ERR_MSG_ENTITY_IN_TRASH_TEMPLATE = "Entity %s is in trash can.";
	public static final String ERR_MSG_ACCESS_DENIED = "Access denied.";
	public static final String ERR_MSG_CANNOT_REMOVE_ACL_OF_PROJECT = "Cannot remove the ACL of a project";

	
}
