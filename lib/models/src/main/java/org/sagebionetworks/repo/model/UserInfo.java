package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.model.util.UserGroupUtil;

/**
 *  Contains both a user and the groups to which she belongs.
 */
public class UserInfo {
	// ALL the groups the user belongs to, except "Public",
	// which everyone implicitly belongs to, and "Administrators",
	// which is encoded in the 'isAdmin' field
	private Collection<UserGroup> groups;

	// The user's individual group
	private UserGroup individualGroup; 

	private final boolean isAdmin;

	public UserInfo(boolean isAdmin) {this.isAdmin = isAdmin;}

	public boolean isAdmin() {return isAdmin;}


	public Collection<UserGroup> getGroups() {
		return groups;
	}

	public void setGroups(Collection<UserGroup> groups) {
		this.groups = groups;
	}

	public UserGroup getIndividualGroup() {
		return individualGroup;
	}

	public void setIndividualGroup(UserGroup individualGroup) {
		this.individualGroup = individualGroup;
	}

	/**
	 * Is the passed userInfo object valid?
	 */
	public static void validateUserInfo(UserInfo info) throws UserNotFoundException {

		if (info == null) throw new IllegalArgumentException("UserInfo cannot be null");

		//User.validateUser(info.getUser());

		UserGroupUtil.validate(info.getIndividualGroup());

		// Validate each group
		Collection<UserGroup> groups = info.getGroups();
		if (groups != null) {
			for (UserGroup group : groups) {
				UserGroupUtil.validate(group);
			}
		}
	}
}
