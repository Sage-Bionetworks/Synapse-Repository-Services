package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
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
	 * use: {@link #createNewNode(Node)}
	 */
	@Deprecated 
	public String createNew(Node node);
	
	/**
	 * Create a new node.
	 * @param node
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public Node createNewNode(Node node);
	
	/**
	 * Create a bootstrap node.
	 * 
	 * @param node
	 * @param id
	 * @return
	 */
	public Node bootstrapNode(Node node, long id);
	
	/**
	 * Create a new version of an existing node.
	 * @param newVersion fields that are left null are unmodified
	 * @return The new current revision number for this node.
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Long createNewVersion(Node newVersion);
	
	/**
	 * Fetch a node using its id.
	 * @param id
	 * @return the node
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public Node getNode(String id);
	
	/**
	 * Get the node for a given version number.
	 * @param id
	 * @param versionNumber
	 * @return the particular version of a node
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public Node getNodeForVersion(String id, Long versionNumber);
	
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
	public void deleteVersion(String id, Long versionNumber);

	/**
	 * Update user annotations.
	 * @param id
	 * @param annotationsV2
	 */
	void updateUserAnnotations(String id, Annotations annotationsV2);

	/**
	 * Update annotations for the node's additional entity properties
	 * @param nodeId
	 * @param updatedAnnos
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	void updateEntityPropertyAnnotations(String nodeId, org.sagebionetworks.repo.model.Annotations updatedAnnos) throws NotFoundException, DatastoreException;

	/**
	 * Get all of the version numbers for this node.
	 * @param id
	 * @return a list of verison numbers
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public List<Long> getVersionNumbers(String id);

	/**
	 * Get user annotations associated with the current version of the entity
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	Annotations getUserAnnotations(String id);

	/**
	 * Get user annotations for a specific version of the entity
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 */
	Annotations getUserAnnotationsForVersion(String id, Long versionNumber);

	/**
	 * Get Entity properties that could not be stored as a Node
	 * @param id
	 * @return Annotations containing the extra properties for the Entity
	 */
	org.sagebionetworks.repo.model.Annotations getEntityPropertyAnnotations(String id);

	/**
	 * Get Entity properties for a specific version that could not be stored as a Node
	 * @param id
	 * @param versionNumber
	 * @return Annotations containing the extra properties for the Entity
	 */
	org.sagebionetworks.repo.model.Annotations getEntityPropertyAnnotationsForVersion(String id, Long versionNumber);

	/**
	 * Look at the current eTag without locking or changing anything.
	 * @param id
	 * @return the current etag
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public String peekCurrentEtag(String id);

	/**
	 * Make changes to an existing node.
	 * @param updatedNode fields that are left null are not modified
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void updateNode(Node updatedNode);
	
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
	 * From the given set of Node IDs, get the sub-set of
	 * nodes that are available.  A node is available if it exists
	 * and is not in the trash.
	 * @param nodeIds
	 * @return
	 */
	public Set<Long> getAvailableNodes(List<Long> nodeIds);
	
	/**
	 * True if the node exists and is not in the trash.
	 * @param nodeId
	 * @return
	 */
	public boolean isNodeAvailable(String nodeId);
	
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
	public EntityType getNodeTypeById(String nodeId);

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
	public String getParentId(String nodeId) throws NumberFormatException, NotFoundException;
	
	/**
	 * Get the current revision number for a node.
	 * @param nodeId
	 * @return the current revision number
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
	public Long getCurrentRevisionNumber(String nodeId);
	
	/**
	 * Get the activity id for the current version of the node. 
	 * @param nodeId
	 * @return
	 */
	public String getActivityId(String nodeId);
	
	/**
	 * Get the activity id for a specific version of the node.
	 * @param nodeId
	 * @param revNumber
	 * @return
	 */
	public String getActivityId(String nodeId, Long revNumber);
	
	/**
	 * Get the Synapse ID of the creator of a node.
	 * @throws DatastoreException 
	 */
	public Long getCreatedBy(String nodeId);
	
	/**
	 * returns true iff the given node is root
	 * 
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public boolean isNodeRoot(String nodeId);

	/**
	 * returns true iff the parent of the given node is root
	 * 
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException 
	 */
    public boolean isNodesParentRoot(String nodeId);

    /**
     * Does this given node have any children?
     * @param nodeId
     * @return
     */
	public boolean doesNodeHaveChildren(String nodeId);

	public List<VersionInfo> getVersionsOfEntity(String entityId, long offset,
			long limit);

	/**
	 * Get the FileHandle Id for a given version number.
	 * 
	 * @param id
	 * @param versionNumber if null, use the current version
	 * @return
	 */
	public String getFileHandleIdForVersion(String id, Long versionNumber);
	
	/**
	 * Get the FileHandleAssociation for the current version of the given entity IDs.
	 * 
	 * Note: If any of the provided entity IDs are not files or do not have a file handle ID,
	 * then a result will not be included for that entity.
	 * 
	 * @param entityIds
	 * @return
	 */
	public List<FileHandleAssociation> getFileHandleAssociationsForCurrentVersion(List<String> entityIds);

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
	public String lockNode(String nodeId);

	
	/**
	 * 
	 * Return one page of ProjectHeader information, based on a (long) list of project IDs,
	 * sorted according to the sortColumn and sortDirection param's.  The project metadata
	 * includes when it was last accessed by the given user.
	 * 
	 * if type is MY_CREATED_PROJECTS then return only projects created by the given user.
	 * if type is MY_PARTICIPATED_PROJECTS then return only projects NOT created by the given user.
	 * 
	 * @param userId user whose activity is to be returned
	 * @param projectIds list of IDs to filter
	 * @param type filter criterion
	 * @param sortColumn sort criterion (last activity time or project name)
	 * @param sortDirection sort direction
	 * @param limit (>=0)
	 * @param offset (>=0)
	 * @return
	 */
	public List<ProjectHeader> getProjectHeaders(Long userId, Set<Long> projectIds,
			ProjectListType type, ProjectListSortColumn sortColumn, SortDirection sortDirection, Long limit, Long offset);


	/**
	 * Return the number of entities in Synapse
	 * @return
	 */
	long getCount();

	/**
	 * Get the IDs of all container nodes within the hierarchy of the given
	 * parent.  This method will make one database call per level of
	 * hierarchy.
	 * 
	 * @param parentId
	 * @param maxNumberOfIds the maximum number of IDs that should be loaded. An attempt to exceed
	 * this maximum will result in a LimitExceededException.
	 * @return The returned List is the IDs of each container within the
	 *         hierarchy. The passed parent ID is the first element. IDs are in
	 *         in ascending order starting with the direct children followed by
	 *         grandchildren etc
	 * @throws LimitExceededException if the number of container IDs loaded would exceed
	 * the passed maxNumberOfIds. 
	 */
	Set<Long> getAllContainerIds(Collection<Long> parentIds, int maxNumberOfIds) throws LimitExceededException;
	
	/**
	 * See: {@link #getAllContainerIds(Long)}
	 * @param parentId
	 * @param maxNumberIds the maximum number of IDs that should be loaded.
	 * @return
	 */
	Set<Long> getAllContainerIds(String parentId, int maxNumberIds) throws LimitExceededException;
	
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
	 * A node's permissions benefactor is the node which its permissions are inherited from.
	 * This is the non-cached version of the node's benefactor.  The returned value is always consistent.
	 * @param beneficiaryId
	 * @return
	 */
	public String getBenefactor(String beneficiaryId);

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

	/**
	 * 
	 * @param parentId
	 * @param includeTypes
	 * @param childIdsToExclude
	 * @param sortBy
	 * @param sortDirection
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<EntityHeader> getChildren(String parentId,
			List<EntityType> includeTypes, Set<Long> childIdsToExclude,
			SortBy sortBy, Direction sortDirection, long limit, long offset);
	
	/**
	 * Get the statistics about the given parentID and types.
	 * 
	 * @param request
	 * @return
	 */
	ChildStatsResponse getChildernStats(ChildStatsRequest request);

	/**
	 * Count the number of children in this container.
	 * 
	 * @param parentId
	 * @return
	 */
	public long getChildCount(String parentId);

	/**
	 * A single page of IDs and types for a given parentIds.
	 * 
	 * @param parentId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<NodeIdAndType> getChildren(String parentId, long limit,
			long offset);

	/**
	 * Retrieve an entityId given its name and parentId.
	 * 
	 * @param parentId
	 * @param entityName
	 * @return
	 */
	public String lookupChild(String parentId, String entityName);

	/**
	 * For each parent, get the sum of CRCs of their children.
	 *   
	 * @return Map.key = parentId and map.value = sum of children CRCs.
	 * 
	 */
	public Map<Long, Long> getSumOfChildCRCsForEachParent(List<Long> parentIds);
	
	/**
	 * Get the Id and Etag of all of the children for the given parentId.
	 * @param parentId
	 * @return
	 */
	public List<IdAndEtag> getChildren(long parentId);
	
	/**
	 * Touch the node and change the etag, modified on, and modified by.
	 * 
	 * @param userId
	 * @param nodeId
	 * @return
	 */
	public String touch(Long userId, String nodeId);
	
	/**
	 * Touch the node and change the etag, modifiedOn, and modifiedBy.
	 * 
	 * @param userId
	 * @param nodeId
	 * @param changeType The type of change that triggered this update.
	 * @return
	 */
	public String touch(Long userId, String nodeId, ChangeType changeType);
	
	/**
	 * Create a snapshot of the current version, by applying the comment, label, and activity to the 
	 * current version.
	 * @param nodeId
	 * @param request
	 * @return The version number of the the snapshot (will always be the current version number).
	 */
	public long snapshotVersion(Long userId, String nodeId, SnapshotRequest request);
}
