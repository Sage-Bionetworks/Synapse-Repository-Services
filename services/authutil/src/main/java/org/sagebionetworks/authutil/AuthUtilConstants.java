package org.sagebionetworks.authutil;

public class AuthUtilConstants {
	
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

}
