package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.FileHandleUpdateRequest;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeManager {

	/**
	 * Use: {@link #createNode(Node, UserInfo)}
	 */
	@Deprecated
	public String createNewNode(Node newNode, UserInfo userInfo) throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException;
	
	/**
	 * Create a new node.
	 * @param newNode
	 * @param userInfo
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Node createNode(Node newNode, UserInfo userInfo) throws DatastoreException,InvalidModelException, NotFoundException, UnauthorizedException;
	
	/**
	 * Create a new node with annotations.
	 * @param newNode
	 * @param newAnnotations
	 * @param userInfo
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Node createNewNode(Node newNode, org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations, UserInfo userInfo) throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException;
	
	/**
	 * Delete a node using its id. For internal use only. This method should never be exposed from the API directly or indirectly.
	 * If the node is a container with more than 15 level of depth it would fail with a DB exception.
	 *  
	 * 
	 * @param userName
	 * @param nodeId
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void delete(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get a node using its id.
	 * @param userName
	 * @param nodeId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Node getNode(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get a node without an authorization check.
	 * @param nodeId
	 * @return
	 */
	public Node getNode(String nodeId);
	
	/**
	 * Get the full path of a node.
	 * 
	 * @param userInfo
	 * @param nodeId
	 * @return The first EntityHeader in the list will be the root parent for this node, and the last
	 * will be the EntityHeader for the given node.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getNodePath(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * This should only be called for internal use.
	 * @param userInfo
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getNodePathAsAdmin(String nodeId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get a node for a given version number.
	 * @param userInfo
	 * @param nodeId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Node getNodeForVersionNumber(UserInfo userInfo, String nodeId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Update a node and its annotations in the same call.  This means we only need to acquire the lock once.
	 * @param username
	 * @param updatedAnnoations
	 * @param updatedNode
	 * @param newVersion - Should a new version be created for this update?
	 * @throws UnauthorizedException
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public Node update(UserInfo userInfo, Node updatedNode, org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations, boolean newVersion) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	/**
	 * Updates the file handle id of the node revision with the given id and version. The node must be a
	 * {@link EntityType#file file} node and the MD5 of the old file handle must match the MD5 of the new 
	 * file handle. The update will fail if either the old or new file handles do not have an MD5 set.
	 * 
	 * @param userInfo      The user performing the update
	 * @param nodeId        The id of a node of type {@link EntityType#file file}
	 * @param versionNumber The version number
	 * @param request       The update request
	 * @throws NotFoundException          If a node of type file with the given id does not exist, if the revision does
	 *                                    not exist or if the file handle does not exits
	 * @throws ConflictingUpdateException If the {@link FileHandleUpdateRequest#getOldFileHandleId()} does not match the
	 *                                    node revision file handle id, or if the MD5 of the old and new file handle
	 *                                    does not match
	 * @throws UnauthorizedException      If the user is not authorized to read or update the given entity or if the
	 *                                    {@link FileHandleUpdateRequest#getNewFileHandleId()} is not owned by the user
	 */
	void updateNodeFileHandle(UserInfo userInfo, String nodeId, Long versionNumber, FileHandleUpdateRequest updateRequest);
	
	/**
	 * Update the user annotations of a node.
	 * @param userInfo
	 * @param nodeId
	 * @return
	 * @throws ConflictingUpdateException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws InvalidModelException 
	 */
	public Annotations updateUserAnnotations(UserInfo userInfo, String nodeId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	Annotations getUserAnnotations(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the user annotations without an authorization check.
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	Annotations getUserAnnotations(String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;

	Annotations getUserAnnotationsForVersion(UserInfo userInfo, String nodeId, Long versionNumber) throws NotFoundException,
			DatastoreException, UnauthorizedException;

	org.sagebionetworks.repo.model.Annotations getEntityPropertyAnnotations(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	org.sagebionetworks.repo.model.Annotations getEntityPropertyAnnotations(String nodeId);

	org.sagebionetworks.repo.model.Annotations getEntityPropertyForVersion(UserInfo userInfo, String nodeId, Long versionNumber) throws NotFoundException,
			DatastoreException, UnauthorizedException;

	/**
	 * Get the node type of an entity
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public EntityType getNodeType(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the EntityType without an authorization call. 
	 * @param entityId
	 * @return
	 */
	EntityType getNodeType(String entityId);;
	
	/**
	 * Get the node type of an entity for deletion
	 * 
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public EntityType getNodeTypeForDeletion(String entityId) throws NotFoundException, DatastoreException,
			UnauthorizedException;

	/**
	 * Get a full header for an entity.
	 * 
	 * @param userInfo
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public EntityHeader getNodeHeader(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get an entity header for each reference.
	 * 
	 * @param userInfo
	 * @param references
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getNodeHeader(UserInfo userInfo, List<Reference> references) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Gets the header information for entities whose file's MD5 matches the given MD5 checksum.
	 */
	public List<EntityHeader> getNodeHeaderByMd5(UserInfo userInfo, String md5)
			throws NotFoundException, DatastoreException;

	/**
	 * Delete a specific version of a node.
	 * @param userInfo
	 * @param id
	 * @param long1
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws ConflictingUpdateException 
	 */
	public void deleteVersion(UserInfo userInfo, String id, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Does this node have children?
	 * 
	 * @param entityId
	 * @return
	 */
	public boolean doesNodeHaveChildren(String entityId);

	public List<VersionInfo> getVersionsOfEntity(UserInfo userInfo,
			String entityId, long offset, long limit) throws NotFoundException, UnauthorizedException, DatastoreException;

	/**
	 * Gets the activity that generated the Node
	 * @param userInfo
	 * @param nodeId
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public Activity getActivityForNode(UserInfo userInfo, String nodeId, Long versionNumber) throws DatastoreException, NotFoundException;

	/**
	 * Sets the activity that generated the current version of the node
	 * @param userInfo
	 * @param nodeId
	 * @param activityId
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public void setActivityForNode(UserInfo userInfo, String nodeId,
			String activityId) throws NotFoundException, UnauthorizedException,
			DatastoreException;

	/**
	 * Deletes the generatedBy relationship between the entity and its activity
	 * @param userInfo
	 * @param nodeId
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public void deleteActivityLinkToNode(UserInfo userInfo, String nodeId)
			throws NotFoundException, UnauthorizedException, DatastoreException;


	/**
	 * Get the FileHandleId of the file associated with a given version of the entity. The caller must have permission
	 * to downlaod this file to get the handle.
	 * 
	 * @param userInfo
	 * @param id
	 * @param versionNumber if null, use current version
	 * @return
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public String getFileHandleIdForVersion(UserInfo userInfo, String id, Long versionNumber) throws NotFoundException, UnauthorizedException;

	/**
	 * Get a reference for the current version of the given node ids
	 * @param nodeIds node ids to lookup
	 * @return list of References with the current version filled in
	 */
	public List<Reference> getCurrentRevisionNumbers(List<String> nodeIds);

	/**
	 * Given a list of EntityHeaders, return the sub-set of EntityHeaders that the user is authorized to read.
	 * @param userInfo
	 * @param toFilter
	 * @return
	 */
	List<EntityHeader> filterUnauthorizedHeaders(UserInfo userInfo,
			List<EntityHeader> toFilter);

	/**
	 * Lookup an Entity ID using an alias.
	 * @param alias
	 * @return
	 */
	public String getEntityIdForAlias(String alias);

	/**
	 * Return a set of fileHandleIds that associated with entityId and appear in the provided list.
	 * 
	 * @param fileHandleIds
	 * @param entityId
	 * @return
	 */
	public Set<String> getFileHandleIdsAssociatedWithFileEntity(List<String> fileHandleIds, String entityId);

	/**
	 * Get one page of children for a given parentId
	 * @param parentId The id of the parent.
	 * @param includeTypes The types of children to include in the results.
	 * @param childIdsToExclude Child IDs to be excluded from the results.
	 * @param sortBy Sort by. 
	 * @param sortDirection Sort direction
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<EntityHeader> getChildren(String parentId,
			List<EntityType> includeTypes, Set<Long> childIdsToExclude, SortBy sortBy, Direction sortDirection, long limit, long offset);
	
	/**
	 * Get the statistics for the given parentId and types.
	 * 
	 * @param request
	 * @return
	 */
	public ChildStatsResponse getChildrenStats(ChildStatsRequest request);

	/**
	 * Retrieve the entityId for a given parentId and entityName
	 * 
	 * @param parentId
	 * @param entityName
	 * @return
	 */
	public String lookupChild(String parentId, String entityName);
	
	
	/**
	 * Request to create a new snapshot of a table or view. The provided comment,
	 * label, and activity ID will be applied to the current version thereby
	 * creating a snapshot and locking the current version. After the snapshot is
	 * created a new version will be started with an 'in-progress' label.
	 * 
	 * @param userId
	 * @param nodeId
	 * @param comment  Optional. Version comment.
	 * @param label    Optional. Version label.
	 * @param activity Optional. Associate an activity with the new version.
	 * @return The version number that represents the snapshot/
	 */
	public long createSnapshotAndVersion(UserInfo userInfo, String nodeId, SnapshotRequest request);

	/**
	 * Get the current revision number for the given Entity Id.
	 * @param entityId
	 * @return
	 */
	long getCurrentRevisionNumber(String entityId);

	/**
	 * Get the name of the given node.
	 * @param userInfo
	 * @param nodeId
	 * @return
	 */
	public String getNodeName(UserInfo userInfo, String nodeId);

	/**
	 * Find the first bound JSON schema for the given nodeId.
	 * @param id
	 * @return
	 */
	public Long findFirstBoundJsonSchema(Long nodeId);

}
