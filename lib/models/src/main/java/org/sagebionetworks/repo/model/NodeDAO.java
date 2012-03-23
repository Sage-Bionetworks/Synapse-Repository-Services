package org.sagebionetworks.repo.model;

import java.util.LinkedHashSet;
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
	 * @param node
	 * @return the new node's id
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws NumberFormatException 
	 */
	public String createNew(Node node) throws NotFoundException, DatastoreException;
	
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
	 * @return the node
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public Node getNode(String id) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the node for a given version number.
	 * @param id
	 * @param versionNumber
	 * @return the particular version of a node
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public Node getNodeForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException;
	
	/**
	 * Delete a node using its id.
	 * @param id
	 * @return boolean
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public boolean delete(String id) throws NotFoundException, DatastoreException;
	
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
	 * @return the current annotations
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public NamedAnnotations getAnnotations(String id) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the annotations for a given version number
	 * @param id
	 * @param versionNumber 
	 * @return the version-specific annotations
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public NamedAnnotations getAnnotationsForVersion(String id, Long versionNumber) throws NotFoundException, DatastoreException;
	
	/**
	 * Get all of the children nodes of a given node.
	 * @param id
	 * @return the child nodes
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public Set<Node> getChildren(String id) throws NotFoundException, DatastoreException;
	
	/**
	 * Get all of the version numbers for this node.
	 * @param id
	 * @return a list of verison numbers
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public List<Long> getVersionNumbers(String id) throws NotFoundException, DatastoreException;
	
	/**
	 * Get all of the IDs for a given node's children
	 * @param id
	 * @return the set of child ids
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public Set<String> getChildrenIds(String id) throws NotFoundException, DatastoreException;
	
	/**
	 * Look at the current eTag without locking or changing anything.
	 * @param id
	 * @return the current etag
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public String peekCurrentEtag(String id) throws NotFoundException, DatastoreException;
	
	/**
	 * Lock the given node using 'SELECT FOR UPDATE', and increment the etag.
	 * @param id
	 * @param eTag - The current eTag for this node.  If this eTag does not match the current
	 * eTag, then ConflictingUpdateException will be thrown.
	 * @return the new etag
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
	 * @throws DatastoreException 
	 */
	public void updateNode(Node updatedNode) throws NotFoundException, DatastoreException;
	
	/**
	 * Update a nodes annotations.
	 * @param nodeId 
	 * @param updatedAnnos 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void updateAnnotations(String nodeId, NamedAnnotations updatedAnnos) throws NotFoundException, DatastoreException;

	
	/**
	 * Does a given node exist?
	 * @param nodeId
	 * @return whether or not the node exists
	 */
	public boolean doesNodeExist(Long nodeId);
	
	/**
	 * Get the header information for an entity.
	 * @param nodeId
	 * @return the entity header
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
	
	/**
	 * Bootstrap all node types.
	 * @throws DatastoreException 
	 */
	public void boostrapAllNodeTypes() throws DatastoreException;
	
	/**
	 * Lookup a node id using its unique path.
	 * @param path
	 * @return the node id
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public String getNodeIdForPath(String path) throws DatastoreException, NotFoundException;

	/**
	 * Get the ordered list of children ids.
	 * @param id
	 * @return list of child ids
	 * @throws DatastoreException 
	 */
	public List<String> getChildrenIdsAsList(String id) throws DatastoreException;

	/**
	 * Does this revision already exist?
	 * @param nodeId
	 * @param revNumber
	 * @return whether or not the revision exists
	 */
	boolean doesNodeRevisionExist(String nodeId, Long revNumber);
	
	/**
	 * Only the annotations of the current version are query-able.
	 * @param nodeId
	 * @param annotationKey
	 * @return whether or not the annotation is queryable
	 * @throws DatastoreException 
	 */
	public boolean isStringAnnotationQueryable(String nodeId, String annotationKey) throws DatastoreException;
	
	/**
	 * Returns string of parentId for a Node
	 * @param nodeId
	 *@return String node's parentId
	 * @throws NotFoundException 
	 * @throws NumberFormatException 
	 * @throws DatastoreException 
	 */
	public String getParentId(String nodeId) throws NumberFormatException, NotFoundException, DatastoreException;
	
	/**
	 * Handles change to a parentId for a node and saves reference to new parent in 
	 * database.
	 * @param nodeId
	 * @param newParentId
	 * @return returns true if the parent was actually changed, false if not.  So, if parent was
	 * already set to the parameter newParentId then it will return false.
	 * @throws NotFoundException 
	 * @throws NumberFormatException 
	 * @throws DatastoreException 
	 */
	public boolean changeNodeParent(String nodeId, String newParentId) throws NumberFormatException, NotFoundException, DatastoreException;

	/**
	 * Update an existing node using a backup
	 * @param node
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void updateNodeFromBackup(Node node) throws NotFoundException, DatastoreException;

	/**
	 * Create a new node from a backup.
	 * @param node
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void createNewNodeFromBackup(Node node) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the current revision number for a node.
	 * @param nodeId
	 * @return the current revision number
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public Long getCurrentRevisionNumber(String nodeId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get all of the node types for a given alias.
	 * @param alias
	 * @return
	 */
	public List<Short> getAllNodeTypesForAlias(String alias);

}
