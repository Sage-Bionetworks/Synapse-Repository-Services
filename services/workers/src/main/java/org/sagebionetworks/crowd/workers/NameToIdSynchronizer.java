package org.sagebionetworks.crowd.workers;

import java.util.Collection;

import org.sagebionetworks.ids.NamedIdGenerator;
import org.sagebionetworks.ids.NamedIdGenerator.NamedType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;

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
			namedIdGenerator.unconditionallyAssignIdToName(Long.parseLong(ug.getId()), ug.getName(), NamedType.USER_GROUP_ID);
		}
	}
	
}
