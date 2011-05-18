package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class TempMockAuthDao implements AuthorizationManager{




	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.AuthorizationManager#canAccess(org.sagebionetworks.repo.model.UserInfo, java.lang.String, org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE)
	 */
	@Override
	public boolean canAccess(UserInfo userInfo, String nodeId,
			ACCESS_TYPE accessType) throws NotFoundException,
			DatastoreException {
		// TODO Auto-generated method stub
		return true;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.AuthorizationManager#canCreate(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public boolean canCreate(UserInfo userInfo, String nodeType)
			throws NotFoundException, DatastoreException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void removeAuthorization(String nodeId) throws NotFoundException,
			DatastoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String authorizationSQL(ACCESS_TYPE accessType, List<String> groupIds) {
		// TODO Auto-generated method stub
		return null;
	}



}
