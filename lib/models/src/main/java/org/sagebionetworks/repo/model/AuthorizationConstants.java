/**
 * 
 */
package org.sagebionetworks.repo.model;

/**
 * @author deflaux
 * 
 */
public class AuthorizationConstants {
	
	public enum ACCESS_TYPE {
		READ,
		CHANGE,
		SHARE
	};
	
//	public static void main(String[] args) throws Exception {
//		ACCESS_TYPE type = ACCESS_TYPE.READ;
//		String strType = type.name(); // for writing to database
//		System.out.println(strType);
//		ACCESS_TYPE type2 = ACCESS_TYPE.valueOf("foo"); // throws exception
//	}


//	/**
//	 * Read-only access
//	 */
//	public static final String READ_ACCESS = "read";
//
//	/**
//	 * Read-write access
//	 */
//	public static final String CHANGE_ACCESS = "change";
//
//	/**
//	 * Gives the group having this permission the ability to 
//	 * extend access on the group's resources in turn to other groups.
//	 */
//	public static final String SHARE_ACCESS = "share";

	/**
	 * The group name for a system defined group which allows access to its
	 * resources to all (including anonymous users)
	 */
	public static final String PUBLIC_GROUP_NAME = "Public";
	
	/**
	 * The group name for those users that have all kinds of access to all resources.
	 */
	public static final String ADMIN_GROUP_NAME = "Administrators";
	

}
