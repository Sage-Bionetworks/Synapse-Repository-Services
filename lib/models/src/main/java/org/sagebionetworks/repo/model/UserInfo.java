package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Iterator;

/**
 *  A class meant to contain both a user and the groups to which she belongs
 */
public class UserInfo {
	private User user;
	private Collection<UserGroup> groups; // ALL the groups the user belongs to, 
						// except "Public", which everyone implicitly belongs to
	private UserGroup individualGroup; // the user's individual group
	private boolean isAdmin;
	
	public UserInfo(boolean isAdmin) {setAdmin(isAdmin);}
	
	/**
	 * @return the isAdmin
	 */
	public boolean isAdmin() {
		return isAdmin;
	}
	/**
	 * @param isAdmin the isAdmin to set
	 */
	private  void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	/**
	 * @return the user
	 */
	public User getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}
	
	/**
	 * @return the groups
	 */
	public Collection<UserGroup> getGroups() {
		return groups;
	}
	/**
	 * @param groups the groups to set
	 */
	public void setGroups(Collection<UserGroup> groups) {
		this.groups = groups;
	}
	/**
	 * @return the individualGroup
	 */
	public UserGroup getIndividualGroup() {
		return individualGroup;
	}
	/**
	 * @param individualGroup the individualGroup to set
	 */
	public void setIndividualGroup(UserGroup individualGroup) {
		this.individualGroup = individualGroup;
	}
	
	/**
	 * Is the passed userInfo object valid?
	 * @param info
	 */
	public static void validateUserInfo(UserInfo info){
		if(info == null) throw new IllegalArgumentException("UserInfo cannot be null");
		User.validateUser(info.getUser());
		UserGroup.validate(info.getIndividualGroup());
		// Validate each group
		Collection<UserGroup> groups = info.getGroups();
		if(groups != null){
			Iterator<UserGroup> it = groups.iterator();
			while(it.hasNext()){
				UserGroup.validate(it.next());
			}
		}
	}
	
}
