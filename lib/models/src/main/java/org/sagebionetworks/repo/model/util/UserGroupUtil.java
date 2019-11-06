package org.sagebionetworks.repo.model.util;

import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserNotFoundException;

public class UserGroupUtil {
	
	/**
	 * Is the passed UserGroup valid?
	 * @param userGroup
	 */
	public static void validate(UserGroup userGroup) throws UserNotFoundException {
		if (userGroup == null) throw new IllegalArgumentException("UserGroup cannot be null");
		if (userGroup.getId() == null) throw new UserNotFoundException("UserGroup.id cannot be null");
		if (userGroup.getIsIndividual() == null) throw new UserNotFoundException("UserGroup.isIndividual cannot be null");
	}
}
