package org.sagebionetworks.repo.manager;

import java.util.Collection;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.web.NotFoundException;



public interface NodeManager {
	
	/**
	 * Create a new node
	 * @param user
	 * @param newNode
	 * @return
	 */
	public String createNewNode(Node newNode, String userName) throws DatastoreException,
			InvalidModelException;


	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @param user
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Node get(String id, String userName) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * This updates the 'shallow' properties of an object
	 * 
	 * @param node
	 *            non-null id is required
	 * @param user
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void update(Node dto, String userName) throws DatastoreException, InvalidModelException,
			NotFoundException, UnauthorizedException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id, String userName) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	public boolean hasAccess(Node resource, String accessType, String userName) throws NotFoundException, DatastoreException ;

}
