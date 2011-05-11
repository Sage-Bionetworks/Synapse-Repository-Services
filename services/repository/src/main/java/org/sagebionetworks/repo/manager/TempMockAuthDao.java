package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.web.NotFoundException;

public class TempMockAuthDao implements AuthorizationManager{

	@Override
	public User createUser(String userName) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteUser(String userName) throws DatastoreException,
			NotFoundException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canAccess(String userName, String nodeId,
			ACCESS_TYPE accessType) throws NotFoundException,
			DatastoreException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void addUserAccess(Node node, String userName)
			throws NotFoundException, DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAuthorization(String nodeId) throws NotFoundException,
			DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canCreate(String userName, String nodeType)
			throws NotFoundException, DatastoreException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String authorizationSQL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAdmin(User user) throws DatastoreException,
			NotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAdmin(String userName) throws DatastoreException,
			NotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

}
