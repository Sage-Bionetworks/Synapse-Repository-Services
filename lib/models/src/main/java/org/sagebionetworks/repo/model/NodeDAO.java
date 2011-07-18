package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;


/**
 * Interface for all Node C.R.U.D. operations.
 * 
 * @author jmhill
 *
 */
public interface NodeDAO {
	
	/**
	 * Create a new node.
	 * @param parentId can be null, or an existing parent node, to add this node to.
	 * @param node
	 * @return
	 * @throws NotFoundException 
	 * @throws NumberFormatException 
	 */
	public String createNew(Node node) throws NotFoundException;
	
	/**
	 * Create a new version of an existing node.
	 * @param newVersion
	 * @return The new current revision number for this node.
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Long createNewVersion(Node newVersion) throws NotFoundException, DatastoreException;
	
	/**
	 * Fetch a node using its id.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public Node getNode(String id) throws NotFoundException;
	
	/**
	 * Get the node for a given version number.
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public Node getNodeForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException;
	
	/**
	 * Delete a node using its id.
	 * @param id
	 * @throws NotFoundException 
	 */
	public void delete(String id) throws NotFoundException;
	
	/**
	 * Delete a specific version.
	 * @param id
	 * @param versionNumber
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public void deleteVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the Annotations for a node. 
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public Annotations getAnnotations(String id) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the annotations for a given version number
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public Annotations getAnnotationsForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException;
	
	/**
	 * Get all of the children nodes of a given node.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public Set<Node> getChildren(String id) throws NotFoundException;
	
	/**
	 * Get all of the version numbers for this node.
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	public List<Long> getVersionNumbers(String id) throws NotFoundException;
	
	/**
	 * Get all of the IDs for a given node's children
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	public Set<String> getChildrenIds(String id) throws NotFoundException;
	
	/**
	 * Look at the current eTag without locking or changing anything.
	 * @param id
	 * @return
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public String peekCurrentEtag(String id) throws NotFoundException, DatastoreException;
	
	/**
	 * Lock the given node using 'SELECT FOR UPDATE', and increment the etag.
	 * @param id
	 * @param eTag - The current eTag for this node.  If this eTag does not match the current
	 * eTag, then ConflictingUpdateException will be thrown.
	 * @return
	 * @throws NotFoundException - Thrown if the node does not exist.
	 * @throws ConflictingUpdateException - Thrown if the passed eTag does not match the current eTag.
	 * This exception indicates that the node has changed since the last time the user fetched it.
	 * @throws DatastoreException 
	 */
	public String lockNodeAndIncrementEtag(String id, String eTag) throws NotFoundException, ConflictingUpdateException, DatastoreException;
	
	
	/**
	 * Make changes to an existing node.
	 * @param updatedNode
	 * @throws NotFoundException 
	 */
	public void updateNode(Node updatedNode) throws NotFoundException;
	
	/**
	 * Update a nodes annotations.
	 * @param id
	 * @param updatedAnnotations
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void updateAnnotations(String nodeId, Annotations updatedAnnotations) throws NotFoundException, DatastoreException;
	
	/**
	 * Does a given node exist?
	 * @param nodeId
	 * @return
	 */
	public boolean doesNodeExist(Long nodeId);
	
	/**
	 * Get the header information for an entity.
	 * @param nodeId
	 * @return
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public EntityHeader getEntityHeader(String nodeId) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the full path for an entity.
	 * @param nodeId
	 * @return The first EntityHeader in the list will be the root parent for this node, and the last
	 * will be the EntityHeader for the given node.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<EntityHeader> getEntityPath(String nodeId) throws DatastoreException, NotFoundException;
	

}
