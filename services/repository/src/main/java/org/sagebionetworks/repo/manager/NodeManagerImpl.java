package org.sagebionetworks.repo.manager;

import java.util.Collection;

import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Sage business logic for node management.
 * @author jmhill
 *
 */
@Transactional(readOnly = true)
public class NodeManagerImpl implements NodeManager {
	
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	AuthorizationDAO authorizationDao;
	
	@Override
	public String createNewNode(User user, Node newNode) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	

	/**
	 * @param node
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(Node node, String userId) throws DatastoreException,
			InvalidModelException, UnauthorizedException {
		nodeDao.createNew(node);
		return node.getId();
	}

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Node get(String id, String userId) throws DatastoreException, NotFoundException, UnauthorizedException {
		throw new RuntimeException("Not yet implemented.");
	}

	/**
	 * This updates the 'shallow' properties of an object
	 * 
	 * @param node
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void update(Node node, String userId) throws DatastoreException, InvalidModelException,
			NotFoundException, UnauthorizedException {
				throw new RuntimeException("Not yet implemented.");
			}

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id, String userId) throws DatastoreException, NotFoundException, UnauthorizedException {
		throw new RuntimeException("Not yet implemented.");
	}

	/**
	 * Use case:  Need to find out who has authority to add a new user to a group.
	 * Here the 'resource' refers to the group and 'accessType' = 'change'.  The method
	 * would return the administrative group who can modify the group of interest.
	 * 
	 * @param resource the resource of interest
	 * @param accessType a type of access to the object
	 * @return those user groups that have the specified type of access to the given object
	 */
	public Collection<UserGroup> whoHasAccess(Node resource, String accessType) throws NotFoundException, DatastoreException  {
		throw new RuntimeException("Not yet implemented.");
	}
	
	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	public boolean hasAccess(Node resource, String accessType, String userId) throws NotFoundException, DatastoreException  {
		throw new RuntimeException("Not yet implemented.");
	}

}
