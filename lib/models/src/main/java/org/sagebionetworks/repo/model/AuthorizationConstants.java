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
		CREATE, // i.e. permission to add a child to a Node
		READ,
		UPDATE,
		DELETE,
		CHANGE_PERMISSIONS
	};
	
	/**
	 * These are default groups that are guaranteed to be there.
	 */
	public enum DEFAULT_GROUPS {
		AUTHENTICATED_USERS,
		PUBLIC;
		
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
	public static final String PUBLIC_GROUP_NAME = DEFAULT_GROUPS.PUBLIC.name();
	
	/**
	 * The group name for those users that have all kinds of access to all resources.
	 */
	public static final String ADMIN_GROUP_NAME = "Administrators";
	
	/**
	 * The reserved userId for an anonymous user.
	 */
	public static final String ANONYMOUS_USER_ID = "anonymous";

	

}
