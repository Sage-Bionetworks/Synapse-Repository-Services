package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;

import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;

public class JDOAuthorizationDAOImpl implements AuthorizationDAO {
	
	@Autowired
	private JdoTemplate jdoTemplate;

	
	/**
	 * @param groupId
	 * @param nodeId
	 * @param accessTypes
	 * @param userId the user making the request
	 * 
	 * sets the access that given group has for the given node
	 * 
	 * @exception NotFoundException if the group or node is invalid; 
	 * UnauthorizedException if the given user doesn't have 'share'
	 * permission on the given node
	 * 
	 */
	public void setAccess(String groupId, String nodeId, Collection<String> accessTypes, String userId)
		throws NotFoundException, UnauthorizedException, DatastoreException {
		throw new RuntimeException("Not yet implemented.");
	}
	
	/**
	 * @param groupId
	 * @param nodeId
	 * @param userId the user making the request
	 * 
	 * removes the access to the given node from given group
	 * 
	 * @exception NotFoundException if the group or node is invalid; 
	 * UnauthorizedException if the given user doesn't have 'share'
	 * permission on the given node
	 * 
	 */
	public void removeAccess(String groupId, String nodeId, String userId) 
	throws NotFoundException, UnauthorizedException, DatastoreException {
		throw new RuntimeException("Not yet implemented.");
	}
	
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
	public boolean canAccess(String groupId, String nodeId, String accessType) 
		throws NotFoundException, DatastoreException {
			throw new RuntimeException("Not yet implemented.");
		}

}
