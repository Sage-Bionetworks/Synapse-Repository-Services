package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeManager {
	
	/**
	 * Create a new no
	 * @param userId
	 * @param newNode
	 * @return
	 */
	public String createNewNode(Node newNode, String userName) throws DatastoreException,
			InvalidModelException, NotFoundException;

	/**
	 * Delete a node using its id.
	 * @param userName
	 * @param nodeId
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void delete(String userName, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get a node using its id.
	 * @param userName
	 * @param nodeId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Node get(String userName, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Update a node using the provided node.
	 * @param userName
	 * @param updated
	 * @return 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Node update(String userName, Node updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	public boolean hasAccess(Node resource, String accessType, String userName) throws NotFoundException, DatastoreException ;
	
	/**
	 * Get the annotations for a node
	 * @param username
	 * @param nodeId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Annotations getAnnotations(String username, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Update the annotations of a node.
	 * @param username
	 * @param nodeId
	 * @return
	 * @throws ConflictingUpdateException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Annotations updateAnnotations(String username, String nodeId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException;

	
	
	
	

	

}
