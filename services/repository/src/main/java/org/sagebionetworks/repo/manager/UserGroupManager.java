package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembershipDAO;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserGroupManager {

	public UserInfo getUserInfo(String userName) throws DatastoreException, NotFoundException;
	
	// for testing
	public void setUserDAO(UserDAO userDAO);
	public void setGroupMembershipDAO(GroupMembershipDAO groupMembershipDAO);

	

}
