package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AnnotationNameSpace;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeManager {

	public enum FileHandleReason {
		FOR_PREVIEW_DOWNLOAD, FOR_FILE_DOWNLOAD, FOR_HANDLE_VIEW
	}

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
	public Node createNewNode(Node newNode, NamedAnnotations annos, UserInfo userInfo) throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException;
	
	/**
	 * Delete a node using its id.
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
	public Node get(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
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
	 * Update a node using the provided node.
	 * @param userName
	 * @param updated
	 * @return 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws Exception 
	 */
	public Node update(UserInfo userInfo, Node updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	/**
	 * Update a node and its annotations in the same call.  This means we only need to acquire the lock once.
	 * @param username
	 * @param updatedNode
	 * @param updatedAnnoations
	 * @param newVersion - Should a new version be created for this update?
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public Node update(UserInfo userInfo, Node updatedNode, NamedAnnotations annos, boolean newVersion) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	/**
	 * Get the annotations for a node
	 * @param username
	 * @param nodeId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public NamedAnnotations getAnnotations(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the annotations for a given version number.
	 * @param userInfo
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public NamedAnnotations getAnnotationsForVersion(UserInfo userInfo, String nodeId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Update the annotations of a node.
	 * @param username
	 * @param nodeId
	 * @return
	 * @throws ConflictingUpdateException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws InvalidModelException 
	 */
	public Annotations updateAnnotations(UserInfo userInfo, String nodeId, Annotations updated, AnnotationNameSpace namespace) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;
	
	/**
	 * Get a list of all of the version numbers for a node.
	 * @param userInfo
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 */
	public List<Long> getAllVersionNumbersForNode(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;

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
	public EntityHeader getNodeHeader(UserInfo userInfo, String entityId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException;
	
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
	 * Copies the given version to be the current version.
	 */
	public VersionInfo promoteEntityVersion(UserInfo userInfo, String id, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException;

	/**
	 * Get the FileHandleId of the file associated with a given version of the entity. The caller must have permission
	 * to downlaod this file to get the handle.
	 * 
	 * @param userInfo
	 * @param id
	 * @param versionNumber if null, use current version
	 * @param reason the reason the caller requests this filehanlde, used for authorization and capability checks
	 * @return
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public String getFileHandleIdForVersion(UserInfo userInfo, String id, Long versionNumber, FileHandleReason reason)
			throws NotFoundException, UnauthorizedException;

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
	 * Retrieve the entityId for a given parentId and entityName
	 * 
	 * @param parentId
	 * @param entityName
	 * @return
	 */
	public String lookupChild(String parentId, String entityName);

}
