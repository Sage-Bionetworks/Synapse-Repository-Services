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
	
	/**
	 * Creates a new Node object.  
	 * Note:  There is no authorization check.  The caller must perform 
	 * any such checks prior to calling this method.
	 * @param node
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	@Override
	@Transactional(readOnly = false)
	public String createNewNode(Node node, String userName) throws DatastoreException,
			InvalidModelException {
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
	@Override
	public Node get(String id, String userName) throws DatastoreException, NotFoundException, UnauthorizedException {
		if (!authorizationDao.canAccess(userName, id, AuthorizationDAO.READ_ACCESS)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		return nodeDao.getNode(id);
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
	@Override
	@Transactional(readOnly = false)
	public void update(Node node, String userName) throws DatastoreException, InvalidModelException,
			NotFoundException, UnauthorizedException {
		if (!authorizationDao.canAccess(userName, node.getId(), AuthorizationDAO.CHANGE_ACCESS)) {
			throw new UnauthorizedException(userName+" lacks change access to the requested object.");
		}
		nodeDao.updateNode(node);
	}

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Override
	@Transactional(readOnly = false)
	public void delete(String id, String userName) throws DatastoreException, NotFoundException, UnauthorizedException {
		if (!authorizationDao.canAccess(userName, id, AuthorizationDAO.CHANGE_ACCESS)) {
			throw new UnauthorizedException(userName+" lacks change access to the requested object.");
		}
		nodeDao.delete(id);
	}

	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	@Override
	public boolean hasAccess(Node resource, String accessType, String userName) throws NotFoundException, DatastoreException  {
		return authorizationDao.canAccess(userName, resource.getId(), accessType);
	}

}
