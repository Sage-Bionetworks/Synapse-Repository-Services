package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Callback;


/**
 * Interface for all Node C.R.U.D. operations.
 * 
 * @author jmhill
 *
 */
public interface NodeDAO {

	/**
	 * the value to pass into the node to remove the generatedBy link between node and activity
	 * 
	 */
	public static String DELETE_ACTIVITY_VALUE = "-1";
	
	/**
	 * use: {@link #createNewNode(Node)}
	 */
	@Deprecated 
	public String createNew(Node node) throws NotFoundException, DatastoreException, InvalidModelException;
	
	/**
	 * Create a new node.
	 * @param node
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public Node createNewNode(Node node) throws NotFoundException, DatastoreException, InvalidModelException;
	
	/**
	 * Create a new version of an existing node.
	 * @param newVersion fields that are left null are unmodified
	 * @return The new current revision number for this node.
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Long createNewVersion(Node newVersion) throws NotFoundException, DatastoreException, InvalidModelException;
	
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
	public boolean delete(String id) throws DatastoreException;
	
	/**
	 * Delete all nodes within a list of IDs. All nodes in the list must have < 14 levels of children.
	 * @param IDs list of IDs to remove
	 * @return int number of nodes deleted
	 * @throws DatastoreException
	 */
	public int delete(List<Long> IDs) throws DatastoreException;
	
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
	public String lockNodeAndIncrementEtag(String id, String eTag, ChangeType changeType) throws NotFoundException, ConflictingUpdateException, DatastoreException;

	/**
	 * Make changes to an existing node.
	 * @param updatedNode fields that are left null are not modified
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void updateNode(Node updatedNode) throws NotFoundException, DatastoreException, InvalidModelException;
	
	/**
	 * Update a nodes annotations.
	 * @param nodeId 
	 * @param updatedAnnos 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void updateAnnotations(String nodeId, NamedAnnotations updatedAnnos) throws NotFoundException, DatastoreException;
	
	/**
	 * Replace the annotations and fileHandle ID of a specific version of a node.
	 * This is used to convert one entity type to another.
	 * @param nodeId
	 * @param versionNumber
	 * @param updatedAnnos
	 * @param fileHandleId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public void replaceVersion(String nodeId, Long versionNumber, NamedAnnotations updatedAnnos, String fileHandleId) throws NotFoundException, DatastoreException;

	
	/**
	 * Does a given node exist?
	 * Note: If a node is in the trash, this will still return true.
	 * Use {@link #isNodeAvailable()} to find nodes that exist and are not
	 * in the trash.
	 * @param nodeId
	 * @return whether or not the node exists
	 */
	public boolean doesNodeExist(Long nodeId);
	
	/**
	 * True if the node exits and is not in the trash.
	 * @return
	 */
	public boolean isNodeAvailable(Long nodeId);
	
	/**
	 * Get the header information for an entity.
	 * @param nodeId
	 * @return the entity header
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public EntityHeader getEntityHeader(String nodeId, Long versionNumber) throws DatastoreException, NotFoundException;
	
	/**
	 * Get a list of entity headers from a list of references.
	 * @param references
	 * @return
	 */
	public List<EntityHeader> getEntityHeader(List<Reference> references);
	
	/**
	 * Get the headers for the given entity ids.
	 * @param entityIds
	 * @return
	 */
	public List<EntityHeader> getEntityHeader(Set<Long> entityIds);
	
	/**
	 * returns just the entity type for the given node
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public EntityType getNodeTypeById(String nodeId) throws NotFoundException, DatastoreException;

	/**
	 * Gets the header information for entities whose file's MD5 matches the given MD5 checksum.
	 */
	public List<EntityHeader> getEntityHeaderByMd5(String md5) throws DatastoreException, NotFoundException;
	
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
	 * Get the child node of a node by name
	 * 
	 * @param nodeId
	 * @param childName
	 * @return
	 */
	public EntityHeader getEntityHeaderByChildName(String nodeId, String childName) throws DatastoreException, NotFoundException;
	
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
	 * Returns string of parentId for a Node
	 * @param nodeId
	 *@return String node's parentId
	 * @throws NotFoundException 
	 * @throws NumberFormatException 
	 * @throws DatastoreException 
	 */
	public String getParentId(String nodeId) throws NumberFormatException, NotFoundException, DatastoreException;
	
	/**
	 * Handles change to a parentId for a node and saves reference to new parent in database.
	 * 
	 * @param nodeId
	 * @param newParentId
	 * @param isMoveToTrash
	 * @return returns true if the parent was actually changed, false if not. So, if parent was already set to the
	 *         parameter newParentId then it will return false.
	 * @throws NotFoundException
	 * @throws NumberFormatException
	 * @throws DatastoreException
	 */
	public boolean changeNodeParent(String nodeId, String newParentId, boolean isMoveToTrash) throws NumberFormatException,
			NotFoundException, DatastoreException;
	
	/**
	 * Get the current revision number for a node.
	 * @param nodeId
	 * @return the current revision number
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public Long getCurrentRevisionNumber(String nodeId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the activity id for the current version of the node. 
	 * @param nodeId
	 * @return
	 */
	public String getActivityId(String nodeId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the activity id for a specific version of the node.
	 * @param nodeId
	 * @param revNumber
	 * @return
	 */
	public String getActivityId(String nodeId, Long revNumber) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the Synapse ID of the creator of a node.
	 * @throws DatastoreException 
	 */
	public Long getCreatedBy(String nodeId) throws NotFoundException, DatastoreException;
	
	/**
	 * returns true iff the given node is root
	 * 
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public boolean isNodeRoot(String nodeId) throws NotFoundException,
	DatastoreException;

	/**
	 * returns true iff the parent of the given node is root
	 * 
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
    public boolean isNodesParentRoot(String nodeId) throws NotFoundException, DatastoreException;

    /**
     * Does this given node have any children?
     * @param nodeId
     * @return
     */
	public boolean doesNodeHaveChildren(String nodeId);

	public List<VersionInfo> getVersionsOfEntity(String entityId, long offset,
			long limit) throws NotFoundException, DatastoreException;

	public long getVersionCount(String entityId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get a node's reference.
	 * @param nodeId - The id of the node.
	 * @return
	 * @throws NotFoundException - Thrown if the node does not exist.
	 * @throws DatastoreException - Thrown if there is a database error.
	 */
	public Reference getNodeReference(String nodeId) throws NotFoundException, DatastoreException;

	/**
	 * Gets a page of parent relations.
	 */
	QueryResults<NodeParentRelation> getParentRelations(long offset, long limit) throws DatastoreException;

	/**
	 * Get the FileHandle Id for a given version number.
	 * 
	 * @param id
	 * @param versionNumber if null, use the current version
	 * @return
	 */
	public String getFileHandleIdForVersion(String id, Long versionNumber);

	/**
	 * Get a reference for the current version of the given node ids
	 * @param nodeIds node ids to lookup
	 * @return list of References with the current version filled in
	 */
	public List<Reference> getCurrentRevisionNumbers(List<String> nodeIds);
	
	/**
	 * Lock the node and get the current Etag.
	 * @param longId
	 * @return
	 */
	public String lockNode(Long longId);
	
	/**
	 * Lock on a list of node IDs.
	 * Note: The locks will be acquired in the numeric order of the entity IDs to prevent deadlock.
	 * @param longIds
	 * @return
	 */
	public List<String> lockNodes(List<String> nodeIds);

	/**
	 * get a list of projects
	 * 
	 * @param userInfo
	 * @param userToGetInfoFor user to get listing for
	 * @param teamToFetch optional team to get the listing for
	 * @param type type of list
	 * @param sortColumn sort column
	 * @param sortDirection sort direction
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<ProjectHeader> getProjectHeaders(Long userId, Set<Long> projectIds,
			ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection, Long limit, Long offset);


	long getCount();
	
	/**
	 * Update the project Id for a given node and all of its children.
	 * @param nodeId
	 * @param projectId
	 * @return
	 */
	int updateProjectForAllChildren(String nodeId, String projectId);

	/**
	 * Get the IDs of all container nodes within the hierarchy of the given
	 * parent.  This method will make one database call per level of
	 * hierarchy.
	 * 
	 * @param parentId
	 * @return The returned List is the IDs of each container within the
	 *         hierarchy. The passed parent ID is the first element. IDs are in
	 *         in ascending order starting with the direct children followed by
	 *         grandchildren etc
	 */
	List<Long> getAllContainerIds(Long parentId);
	
	/**
	 * Lookup a nodeId using its alias.
	 * @param alias
	 * @return
	 */
	String getNodeIdByAlias(String alias);

	/**
	 * Get the project for the given Entity.
	 * @param objectId
	 * @return
	 */
	public String getProjectId(String objectId);

	/**
	 * Return a set of fileHandleIds that associated with entityId and appear in the provided list.
	 * 
	 * @param fileHandleIds
	 * @param entityId
	 * @return
	 */
	public Set<Long> getFileHandleIdsAssociatedWithFileEntity(List<Long> fileHandleIds, long entityId);
	
	/**
	 * Get the list of all entity DTO objects for the given list of entity ids.
	 * 
	 * @param ids
	 * @param maxAnnotationChars the maximum number of characters for any annotation value.
	 * @return
	 */
	public List<EntityDTO> getEntityDTOs(List<String> ids, int maxAnnotationChars);
}
