package org.sagebionetworks.repo.model.util;

import org.sagebionetworks.repo.model.InvalidUserException;
import org.sagebionetworks.repo.model.UserGroup;

public class UserGroupUtil {
	/**
	 * Is the passed name an email address
	 * @param name
	 * @return
	 */
	public static boolean isEmailAddress(String name){
		if(name == null)throw new IllegalArgumentException("Name cannot be null");
		int index = name.indexOf("@");
		return index > 0;
	}
	
	/**
	 * Is the passed UserGroup valid?
	 * @param userGroup
	 */
	public static void validate(UserGroup userGroup) throws InvalidUserException {

		if (userGroup == null) throw new IllegalArgumentException("UserGroup cannot be null");

		if (userGroup.getId() == null) throw new InvalidUserException("UserGroup.id cannot be null");
		if (userGroup.getName() == null) throw new InvalidUserException("UserGroup.name cannot be null");
		if (userGroup.getIsIndividual() == null) throw new InvalidUserException("UserGroup.isIndividual cannot be null");
		// Only an individual can have an email address for a name
		if (isEmailAddress(userGroup.getName())) {
			if (!userGroup.getIsIndividual()) throw new InvalidUserException(
					"Invalid group name: "+userGroup.getName()+", group names cannot be email addresses");
		} else {
			if (userGroup.getIsIndividual()) throw new InvalidUserException(
					"Invalid user name: "+userGroup.getName()+", user names must be email addresses");
		}
	}
}
