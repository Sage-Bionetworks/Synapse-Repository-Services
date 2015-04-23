package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;


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
	 * Create a new node.
	 * @param node
	 * @return the new node's id
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws NumberFormatException 
	 */
	public String createNew(Node node) throws NotFoundException, DatastoreException, InvalidModelException;
	
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
	public EntityHeader getEntityHeader(String nodeId, Long versionNumber) throws DatastoreException, NotFoundException;

	/**
	 * Gets the header information for entities whose file's MD5 matches the given MD5 checksum.
	 */
	public List<EntityHeader> getEntityHeaderByMd5(String md5) throws DatastoreException, NotFoundException;

	/**
	 * Get the version label for a node
	 * @param nodeId
	 * @param versionNumber
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public String getVersionLabel(String nodeId, Long versionNumber) throws DatastoreException, NotFoundException; 
	
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

	public QueryResults<VersionInfo> getVersionsOfEntity(String entityId, long offset,
			long limit) throws NotFoundException, DatastoreException;

	public long getVersionCount(String entityId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get a node's references.
	 * @param nodeId - The id of the node.
	 * @return
	 * @throws NotFoundException - Thrown if the node does not exist.
	 * @throws DatastoreException - Thrown if there is a database error.
	 */
	public Map<String, Set<Reference>> getNodeReferences(String nodeId) throws NotFoundException, DatastoreException;

	/**
	 * Gets a page of parent relations.
	 */
	QueryResults<NodeParentRelation> getParentRelations(long offset, long limit) throws DatastoreException;

	/**
	 * Get the FileHandle ID for the current version..
	 * 
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String getFileHandleIdForCurrentVersion(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the FileHandle Id for a given version number.
	 * @param id
	 * @param versionNumber
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
	public PaginatedResults<ProjectHeader> getProjectHeaders(UserInfo userInfo, UserInfo userToGetInfoFor, Team teamToFetch,
			ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection, Integer limit, Integer offset);

	long getCount();
}
