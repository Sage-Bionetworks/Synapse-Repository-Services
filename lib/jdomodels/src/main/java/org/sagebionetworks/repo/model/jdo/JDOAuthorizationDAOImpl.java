package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;

public class JDOAuthorizationDAOImpl implements AuthorizationDAO {
	
	@Autowired
	private JdoTemplate jdoTemplate;
	
	/**
	 * @param groupId
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff the given group has the given access to the given node
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	public boolean canAccess(String userName, String nodeId, String accessType) 
		throws NotFoundException, DatastoreException {
		return true;
//			throw new RuntimeException("Not yet implemented.");
		}

}
