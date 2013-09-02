package org.sagebionetworks.crowd.workers;

import java.util.Collection;

import org.sagebionetworks.ids.NamedIdGenerator;
import org.sagebionetworks.ids.NamedIdGenerator.NamedType;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a temporary Synchronizer that ensures the NamedIdGenerator is boostraped with all of the existing user group data.
 * @author John
 *
 */
public class NameToIdSynchronizer implements Runnable {

	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NamedIdGenerator namedIdGenerator;

	@Override
	public void run() {
		// Iterate over all users and groups and ensure that the names are bound to the IDs 
		// in the central ID generator DB.
		Collection<UserGroup> all = userGroupDAO.getAll();
		for(UserGroup ug: all){
			if(!AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME.equals(ug.getName())){
				// We cannot do this for the BOOTSTRAP_USER_GROUP because its ID is zero which is
				// the same a null for MySQL auto-increment columns
				namedIdGenerator.unconditionallyAssignIdToName(Long.parseLong(ug.getId()), ug.getName(), NamedType.USER_GROUP_ID);
			}
		}
	}
	
}
