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
	 * Read-only access
	 */
	public static final String READ_ACCESS = "read";

	/**
	 * Read-write access
	 */
	public static final String CHANGE_ACCESS = "change";

	/**
	 * TODO not sure what this is, whether it includes write permssions or not
	 */
	public static final String SHARE_ACCESS = "share";

	/**
	 * The group name for a system defined group which allows access to its
	 * resources to all (including anonymous users)
	 */
	public static final String PUBLIC_GROUP_NAME = "Public";

}
