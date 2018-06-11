package org.sagebionetworks.repo.manager;
  
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.manager.util.CollectionUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AnnotationNameSpace;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.jdo.AnnotationUtils;
import org.sagebionetworks.repo.model.jdo.EntityNameValidation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.RequesterPaysSetting;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Sage business logic for node management.
 *
 */
public class NodeManagerImpl implements NodeManager {

	private static final String REQUESTER_DOWNLOAD_COPY_NEEDED = "This download is marked for requester pays download. To download this file, follow the instructions as described in https://www.synapse.org/#!Help:RequesterPays";
	static private Log log = LogFactory.getLog(NodeManagerImpl.class);	
	
	private static final Pattern INVALID_ALIAS_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_]");
	
	public static final Long ROOT_ID = KeyFactory.stringToKey(StackConfigurationSingleton.singleton().getRootFolderEntityId());
	public static final Long TRASH_ID = KeyFactory.stringToKey(StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	@Autowired
	NodeDAO nodeDao;	
	@Autowired
	AuthorizationManager authorizationManager;	
	@Autowired
	private AccessControlListDAO aclDAO;	
	@Autowired
	private EntityBootstrapper entityBootstrapper;	

	@Autowired 
	private ActivityManager activityManager;
	@Autowired
	private ProjectSettingsManager projectSettingsManager;
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.NodeManager#createNewNode(org.sagebionetworks.repo.model.Node, org.sagebionetworks.repo.model.UserInfo)
	 */
	@WriteTransaction
	@Override
	@Deprecated
	public String createNewNode(Node newNode, UserInfo userInfo)  throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException {
		newNode = createNode(newNode, userInfo);
		return newNode.getId();
	}
	
	/**
	 * Create a new node
	 */
	@WriteTransaction
	@Override
	public Node createNode(Node newNode, UserInfo userInfo)  throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException {
		// First valid the node
		NodeManagerImpl.validateNode(newNode);
		UserInfo.validateUserInfo(userInfo);
		// Also validate the username
		Long userIndividualGroupId = userInfo.getId();
		// Validate the creations data
		NodeManagerImpl.validateNodeCreationData(userIndividualGroupId, newNode);
		// Validate the modified data.
		NodeManagerImpl.validateNodeModifiedData(userIndividualGroupId, newNode);
		
		// What is the object type of this node
		EntityType type = newNode.getNodeType();
		
		// By default all nodes inherit their ACL from their parent.
		ACL_SCHEME aclScheme = ACL_SCHEME.INHERIT_FROM_PARENT;
		
		// If the user did not provide a parent then we use the default
		if(newNode.getParentId() == null){
			String defaultPath = EntityTypeUtils.getDefaultParentPath(type);
			if(defaultPath == null) throw new IllegalArgumentException("There is no default parent for Entities of type: "+type.name()+" so a valid parentId must be provided"); 
			// Get the parent node.
			String pathId = nodeDao.getNodeIdForPath(defaultPath);
			newNode.setParentId(pathId);
			// Lookup the acl scheme to be used for children of this parent
			aclScheme = entityBootstrapper.getChildAclSchemeForPath(defaultPath);
		}
		
		// check whether the user is allowed to create this type of node
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canCreate(userInfo, newNode.getParentId(), newNode.getNodeType()));
		
		// can this entity be added to the parent?
		validateChildCount(newNode.getParentId(), newNode.getNodeType());

		// Handle permission around file handles.
		if(newNode.getFileHandleId() != null){
			// To set the file handle on a create the caller must have permission 
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccessRawFileHandleById(userInfo, newNode.getFileHandleId()));
			
			if (!authorizationManager.canAccess(userInfo, newNode.getParentId(), ObjectType.ENTITY, ACCESS_TYPE.UPLOAD).getAuthorized()) {
				throw new UnauthorizedException(userInfo.getId().toString()+" is not allowed to upload a file into the chosen folder.");
			}
		}

		// check whether the user is allowed to connect to the specified activity
		canConnectToActivity(newNode.getActivityId(), userInfo);

		// If they are allowed then let them create the node
		newNode = nodeDao.createNewNode(newNode);
		String id = newNode.getId();
		
		// Setup the ACL for this node.
		if(ACL_SCHEME.GRANT_CREATOR_ALL == aclScheme){
			AccessControlList rootAcl = AccessControlListUtil.createACLToGrantEntityAdminAccess(id, userInfo, new Date());
			aclDAO.create(rootAcl, ObjectType.ENTITY);
		}
		
		// adding access is done at a higher level, not here
		//authorizationManager.addUserAccess(newNode, userInfo);
		if(log.isDebugEnabled()){
			log.debug("username: "+userInfo.getId().toString()+" created node: "+id);
		}
		return newNode;
	}
	
	/**
	 * Validate that number of children for this container has not been exceeded.
	 * @param parentId
	 * @param type
	 */
	public void validateChildCount(String parentId, EntityType type) {
		ValidateArgument.required(parentId, "parentId");
		ValidateArgument.required(type, "type");
		// Limits only apply to files, folders, and links
		if (EntityType.file.equals(type) 
				|| EntityType.folder.equals(type)
				|| EntityType.link.equals(type)) {
			// There are no limits on the trash or root
			Long parentIdLong = KeyFactory.stringToKey(parentId);
			if (!ROOT_ID.equals(parentIdLong)
					&& !TRASH_ID.equals(parentIdLong)) {
				// Get the child count
				long currentCount = nodeDao.getChildCount(parentId);
				if (currentCount + 1 > StackConfigurationSingleton.singleton()
						.getMaximumNumberOfEntitiesPerContainer()) {
					throw new IllegalArgumentException(
							"Limit of "
									+ StackConfigurationSingleton.singleton()
											.getMaximumNumberOfEntitiesPerContainer()
									+ " children exceeded for parent: "
									+ parentId);
				}
			}
		}
	}

	/**
	 * Validate a node
	 * @param userName
	 * @param node
	 */
	public static void validateNode(Node node){
		if(node == null) throw new IllegalArgumentException("Node cannot be null");
		if(node.getNodeType() == null) throw new IllegalArgumentException("Node.type cannot be null");
		// If the node name is null then try to use its id
		if(node.getName() == null){
			node.setName(node.getId());
		}
		if(node.getName() == null) throw new IllegalArgumentException("Node.name cannot be null");	
		node.setName(EntityNameValidation.valdiateName(node.getName()));
		if (StringUtils.isNotEmpty(node.getAlias())) {
			if (INVALID_ALIAS_CHARACTERS.matcher(node.getAlias()).find()) {
				throw new IllegalArgumentException("Aliases can only have letters (a-z and A-Z), digits (0-9) and underscores (_)");
			}
		}
	}
	
	/**
	 * Make sure the creation data is set, and if not then set it.
	 * @param userName
	 * @param newNode
	 * @return
	 */
	public static void validateNodeCreationData(Long userIndividualGroupId, Node newNode){
		if(userIndividualGroupId == null) throw new IllegalArgumentException("userIndividualGroupId cannot be null");
		if(newNode == null) throw new IllegalArgumentException("New node cannot be null");
			newNode.setCreatedByPrincipalId(userIndividualGroupId);
			newNode.setCreatedOn(new Date(System.currentTimeMillis()));
	}
	
	/**
	 * When updating a node we clear the 'createdOn' and 'createdBy' fields to tell NodeDAO not to update them.
	 * 
	 * 
	 * @param existingNode
	 */
	public static void clearNodeCreationDataForUpdate(Node existingNode) {
		if(existingNode == null) throw new IllegalArgumentException("Node cannot be null");
		existingNode.setCreatedByPrincipalId(null);
		existingNode.setCreatedOn(null);
	}
	
	/**
	 * Make sure the creation data is set, and if not then set it.
	 * @param userName
	 * @param newNode
	 * @return
	 */
	public static void validateNodeModifiedData(Long userIndividualGroupId, Node newNode){
		if(userIndividualGroupId == null) throw new IllegalArgumentException("Username cannot be null");
		if(newNode == null) throw new IllegalArgumentException("New node cannot be null");
		newNode.setModifiedByPrincipalId(userIndividualGroupId);
		newNode.setModifiedOn(new Date(System.currentTimeMillis()));
	}

	@WriteTransaction
	@Override
	public void delete(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// First validate the username
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getId().toString();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.DELETE));
		nodeDao.delete(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" deleted node: "+nodeId);
		}
		aclDAO.delete(nodeId, ObjectType.ENTITY);
	}
	
	@WriteTransaction
	@Override
	public void deleteVersion(UserInfo userInfo, String id, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// First validate the username
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, id, ObjectType. ENTITY, ACCESS_TYPE.DELETE));
		// Lock before we delete
		String currentETag = nodeDao.peekCurrentEtag(id);
		nodeDao.lockNodeAndIncrementEtag(id, currentETag);
		// Delete while holding the lock.
		nodeDao.deleteVersion(id, versionNumber);
	}
	
	@Override
	public Node get(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate the username
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getId().toString();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, nodeId, ObjectType. ENTITY, ACCESS_TYPE.READ));
		
		Node result = nodeDao.getNode(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" fetched node: "+result.getId());
		}
		return result;
	}

	@Override
	public Node getNodeForVersionNumber(UserInfo userInfo, String nodeId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		Node result = nodeDao.getNodeForVersion(nodeId, versionNumber);
		return result;
	}

	@WriteTransaction
	@Override
	public Node update(UserInfo userInfo, Node updated)
			throws ConflictingUpdateException, NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		return update(userInfo, updated, null, false);
	}

	@WriteTransaction
	@Override
	public Node update(UserInfo userInfo, Node updatedNode, NamedAnnotations updatedAnnos, boolean newVersion)
			throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {

		UserInfo.validateUserInfo(userInfo);
		// Validate that the user can update the node.
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, updatedNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));

		Node oldNode = nodeDao.getNode(updatedNode.getId());

		// note, this is duplicated in 'updateNode', below
		final String parentInDatabase = oldNode.getParentId();
		final String parentInUpdate = updatedNode.getParentId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canUserMoveRestrictedEntity(userInfo, parentInDatabase, parentInUpdate));

		if (!StringUtils.equals(oldNode.getAlias(), updatedNode.getAlias())) {
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(authorizationManager.canChangeSettings(userInfo, oldNode));
		}

		// Validate that the user can assign the file handle if they have it.
		if(updatedNode.getFileHandleId() != null){
			// First determine if this is a change
			String currentHandleId = nodeDao.getFileHandleIdForVersion(updatedNode.getId(), null);
			if(!updatedNode.getFileHandleId().equals(currentHandleId)) {
				// This is a change so the user must be the creator of the new file handle
				AuthorizationManagerUtil.checkAuthorizationAndThrowException(
						authorizationManager.canAccessRawFileHandleById(userInfo, updatedNode.getFileHandleId()));
				AuthorizationManagerUtil.checkAuthorizationAndThrowException(
						authorizationManager.canAccess(userInfo, updatedNode.getParentId(), ObjectType.ENTITY, ACCESS_TYPE.UPLOAD));
			}
		}
		updateNode(userInfo, updatedNode, updatedAnnos, newVersion, ChangeType.UPDATE, oldNode);

		return get(userInfo, updatedNode.getId());
	}

	private void updateNode(UserInfo userInfo, Node updatedNode, NamedAnnotations updatedAnnos, boolean newVersion,
			ChangeType changeType, Node oldNode) throws ConflictingUpdateException, NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {

		NodeManagerImpl.validateNode(updatedNode);

		// Make sure the eTags match
		if (updatedAnnos != null) {
			ValidateArgument.required(updatedNode.getETag(), "eTag");
			if (!updatedNode.getETag().equals(updatedAnnos.getEtag())) {
				throw new IllegalArgumentException("The passed node and annotations do not have the same eTag");
			}
		}

		canConnectToActivity(updatedNode.getActivityId(), userInfo);

		final String nextETag = nodeDao.lockNodeAndIncrementEtag(updatedNode.getId(), updatedNode.getETag(), changeType);

		// Clear node creation data to make sure NodeDAO does not change the fields
		NodeManagerImpl.clearNodeCreationDataForUpdate(updatedNode);

		// Clear the modified data and fill it in with the new data
		Long userIndividualGroupId = userInfo.getId();
		NodeManagerImpl.validateNodeModifiedData(userIndividualGroupId, updatedNode);

		// If this is a new version then we need to create a new version before the update
		if (newVersion) {
			// This will create a new version and set the new version to 
			// be the current version.  Then the rest of the update will then
			// be applied to this new version.
			nodeDao.createNewVersion(updatedNode);
		}

		// Identify if update is a parentId change by comparing our
		// updatedNode's parentId with the parentId in database
		// and update benefactorID/permissions
		final String parentInDatabase = oldNode.getParentId();
		final String parentInUpdate = updatedNode.getParentId();
		final String nodeInUpdate = updatedNode.getId();
		// is this a parentId change?
		if (!KeyFactory.equals(parentInDatabase, parentInUpdate)) {
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccess(userInfo, parentInUpdate, ObjectType.ENTITY, ACCESS_TYPE.CREATE));
			// Validate the limits of the new parent
			validateChildCount(parentInUpdate, updatedNode.getNodeType());
			
			if(NodeUtils.isProjectOrFolder(updatedNode.getNodeType())){
				// Notify listeners of the hierarchy change to this container.
				transactionalMessenger.sendMessageAfterCommit(updatedNode.getId(), ObjectType.ENTITY_CONTAINER, nextETag, ChangeType.UPDATE);
			}
		}

		// Now make the actual update.
		nodeDao.updateNode(updatedNode);

		// Also update the Annotations if provided
		if(updatedAnnos != null){
			updatedAnnos.setEtag(nextETag);
			validateAnnotations(updatedAnnos);
			nodeDao.updateAnnotations(updatedNode.getId(), updatedAnnos);
		}

		if (log.isDebugEnabled()) {
			log.debug("username "+userInfo.getId().toString()+" updated node: "+updatedNode.getId()+", with a new eTag: "+nextETag);
		}
	}

	@Override
	public NamedAnnotations getAnnotations(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getId().toString();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		NamedAnnotations annos = nodeDao.getAnnotations(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" fetched Annotations for node: "+nodeId);
		}
		return annos;
	}

	@Override
	public NamedAnnotations getAnnotationsForVersion(UserInfo userInfo, String nodeId, Long versionNumber) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		return nodeDao.getAnnotationsForVersion(nodeId, versionNumber);
	}

	@WriteTransaction
	@Override
	public Annotations updateAnnotations(UserInfo userInfo, String nodeId, Annotations updated, AnnotationNameSpace namespace) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updated == null) throw new IllegalArgumentException("Annotations cannot be null");
		if(nodeId == null) throw new IllegalArgumentException("Node ID cannot be null");
		UserInfo.validateUserInfo(userInfo);
		// This is no longer called from a create PLFM-325
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
		authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
		// Validate that the annotations
		validateAnnotations(updated);
		// Now lock the node if we can
		nodeDao.lockNodeAndIncrementEtag(nodeId, updated.getEtag());
		NamedAnnotations namedAnnos = nodeDao.getAnnotations(nodeId);
		// Replace a single namespace
		namedAnnos.put(namespace, updated);
		nodeDao.updateAnnotations(nodeId, namedAnnos);
		namedAnnos = getAnnotations(userInfo, nodeId);
		return namedAnnos.getAnnotationsForName(namespace);
	}
	
	public void validateAnnotations(NamedAnnotations updatedAnnos) throws DatastoreException, InvalidModelException {
		if(updatedAnnos == null) throw new IllegalArgumentException("NamedAnnotations cannot be null");
		// Validate all of the annotations
		Iterator<AnnotationNameSpace> it = updatedAnnos.nameIterator();
		while(it.hasNext()){
			validateAnnotations(updatedAnnos.getAnnotationsForName(it.next()));
		}
	}
	
	/**
	 * Validate the passed annotations.  Once a name is used for a type it cannot be used for another type.
	 * @param updated
	 * @throws DatastoreException 
	 * @throws InvalidModelException 
	 */
	public void validateAnnotations(Annotations updated) throws DatastoreException, InvalidModelException{
		if(updated == null) throw new IllegalArgumentException("Annotations cannot be null");
		if(updated.getEtag() == null) throw new IllegalArgumentException("Cannot update Annotations with a null eTag");
		// Validate the annotation names
		AnnotationUtils.validateAnnotations(updated);
	}

	@Override
	public EntityType getNodeType(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		Node node = get(userInfo, nodeId);
		return node.getNodeType();
	}
	
	@Override
	public EntityType getNodeTypeForDeletion(String nodeId) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		Node node = nodeDao.getNode(nodeId);
		return node.getNodeType();
	}

	@Override
	public List<Long> getAllVersionNumbersForNode(UserInfo userInfo,
			String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate that the user can do what they are trying to do.
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		// If they are allowed to read a node then get the list.
		return nodeDao.getVersionNumbers(nodeId);
	}

	@Override
	public List<EntityHeader> getNodePath(UserInfo userInfo, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		return nodeDao.getEntityPath(nodeId);
	}

	@Override
	public List<EntityHeader> getNodePathAsAdmin(String nodeId)	throws NotFoundException, DatastoreException {
		// This version does not require authorization.
		return nodeDao.getEntityPath(nodeId);
	}

	@WriteTransaction
	@Override
	public Node createNewNode(Node newNode, NamedAnnotations newAnnotations, UserInfo userInfo) throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException {
		// First create the node
		newNode = createNode(newNode, userInfo);
		// The eTag really has no meaning yet because nobody has access to this id until we return.
		newAnnotations.setEtag(newNode.getETag());
		newAnnotations.setId(newNode.getId());
		validateAnnotations(newAnnotations);
		// Since we just created this node we do not need to lock.
		nodeDao.updateAnnotations(newNode.getId(), newAnnotations);
		return newNode;
	}

	@Override
	public EntityHeader getNodeHeader(UserInfo userInfo, String entityId, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		return nodeDao.getEntityHeader(entityId, versionNumber);
	}
	
	@Override
	public List<EntityHeader> getNodeHeader(UserInfo userInfo,
			List<Reference> references) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		ValidateArgument.required(references, "references");
		UserInfo.validateUserInfo(userInfo);
		List<EntityHeader> results = nodeDao.getEntityHeader(references);
		// Will remove headers they user cannot see.
		return filterUnauthorizedHeaders(userInfo, results);
	}

	@Override
	public List<EntityHeader> getNodeHeaderByMd5(UserInfo userInfo, String md5)
			throws NotFoundException, DatastoreException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		if (md5 == null) {
			throw new IllegalArgumentException("MD5 cannot be null.");
		}
		List<EntityHeader> entityHeaderList = nodeDao.getEntityHeaderByMd5(md5);
		return filterUnauthorizedHeaders(userInfo, entityHeaderList);
	}
	
	@Override
	public List<EntityHeader> filterUnauthorizedHeaders(UserInfo userInfo, List<EntityHeader> toFilter){
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(toFilter, "toFilter");
		if(toFilter.isEmpty()){
			// nothing to do.
			return toFilter;
		}
		Set<Long> originalBenefactors = new HashSet<Long>(toFilter.size());
		for(EntityHeader header: toFilter){
			ValidateArgument.required(header.getBenefactorId(), "entityHeader.benefactorId");
			originalBenefactors.add(header.getBenefactorId());
		}
		// find the intersection.
		Set<Long> benefactorIntersection = authorizationManager.getAccessibleBenefactors(userInfo, originalBenefactors);
		List<EntityHeader> filtered = new LinkedList<EntityHeader>();
		for(EntityHeader header: toFilter){
			if(benefactorIntersection.contains(header.getBenefactorId())){
				filtered.add(header);
			}
		}
		return filtered;
	}

	@Override
	public boolean doesNodeHaveChildren(String nodeId) {
		return nodeDao.doesNodeHaveChildren(nodeId);
	}

	@Override
	public List<VersionInfo> getVersionsOfEntity(UserInfo userInfo,
			String entityId, long offset, long limit) throws NotFoundException,
			UnauthorizedException, DatastoreException {
		validateReadAccess(userInfo, entityId);
		return nodeDao.getVersionsOfEntity(entityId, offset, limit);
	}

	public void validateReadAccess(UserInfo userInfo, String entityId)
			throws NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.READ));
	}

	@Override
	public Activity getActivityForNode(UserInfo userInfo, String nodeId, Long versionNumber) throws DatastoreException, NotFoundException {
		String activityId = null;
		if(versionNumber != null)
			activityId = nodeDao.getActivityId(nodeId, versionNumber);
		else 
			activityId = nodeDao.getActivityId(nodeId);		
		return activityManager.getActivity(userInfo, activityId);
	}

	@WriteTransaction
	@Override
	public void setActivityForNode(UserInfo userInfo, String nodeId,
			String activityId) throws NotFoundException, UnauthorizedException,
			DatastoreException {
		Node toUpdate = get(userInfo, nodeId);		
		toUpdate.setActivityId(activityId);
		update(userInfo, toUpdate);
	}

	@WriteTransaction
	@Override
	public void deleteActivityLinkToNode(UserInfo userInfo, String nodeId)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		Node toUpdate = get(userInfo, nodeId);
		toUpdate.setActivityId(NodeDAO.DELETE_ACTIVITY_VALUE);
		update(userInfo, toUpdate);
	}	
	
	/*
	 * Private Methods
	 */	
	private void canConnectToActivity(String activityId, UserInfo userInfo) throws NotFoundException {		
		if(activityId != null) {
			if(NodeDAO.DELETE_ACTIVITY_VALUE.equals(activityId)) return;
			if(!activityManager.doesActivityExist(activityId)) 
				throw new NotFoundException("Activity id " + activityId + " not found.");
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccessActivity(userInfo, activityId));
		}
	}

	@WriteTransaction
	@Override
	public VersionInfo promoteEntityVersion(UserInfo userInfo, String nodeId, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
		Long currentVersion = nodeDao.getCurrentRevisionNumber(nodeId);
		if (!currentVersion.equals(versionNumber)) {
			String currentETag = nodeDao.peekCurrentEtag(nodeId);
			nodeDao.lockNodeAndIncrementEtag(nodeId, currentETag);
			Node nodeToPromote = nodeDao.getNodeForVersion(nodeId, versionNumber);
			nodeToPromote.setVersionLabel(null); // To get a new version label
			nodeDao.createNewVersion(nodeToPromote);
		}
		List<VersionInfo> versions = nodeDao.getVersionsOfEntity(nodeId, 0, 1);
		return versions.get(0);
	}

	@Override
	public String getFileHandleIdForVersion(UserInfo userInfo, String id, Long versionNumber, FileHandleReason reason)
			throws NotFoundException, UnauthorizedException {
		// Validate that the user has download permission.
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, id, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD));

		switch (reason) {
		case FOR_FILE_DOWNLOAD:
			// validate that this is not requesterPays
			RequesterPaysSetting requesterPays = projectSettingsManager.getProjectSettingForNode(userInfo, id,
					ProjectSettingsType.requester_pays, RequesterPaysSetting.class);
			if (requesterPays != null && BooleanUtils.isTrue(requesterPays.getRequesterPays())) {
				throw new UnauthorizedException(REQUESTER_DOWNLOAD_COPY_NEEDED);
			}
			break;
		case FOR_PREVIEW_DOWNLOAD:
		case FOR_HANDLE_VIEW:
			break;
		}
		// Get the value from the dao
		String fileHandleId = nodeDao.getFileHandleIdForVersion(id, versionNumber);
		checkFileHandleId(id, fileHandleId);
		return fileHandleId;
	}	

	@Override
	public List<Reference> getCurrentRevisionNumbers(List<String> nodeIds) {
		return nodeDao.getCurrentRevisionNumbers(nodeIds);
	}

	
	/*
	 * Private Methods
	 */
	
	/**
	 * Throws a NotFoundException when a file handle is null
	 * @param entityId
	 * @param fileHandleId
	 * @throws NotFoundException
	 */
	private void checkFileHandleId(String entityId, String fileHandleId)
			throws NotFoundException {
		if(fileHandleId == null) throw new NotFoundException("Object "+entityId+" does not have a file handle associated with it.");
	}

	@Override
	public String getEntityIdForAlias(String alias) {
		return nodeDao.getNodeIdByAlias(alias);
	}

	@Override
	public Set<String> getFileHandleIdsAssociatedWithFileEntity(List<String> fileHandleIds, String entityId) {
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		ValidateArgument.required(entityId, "entityId");
		List<Long> fileHandleIdsLong = new ArrayList<Long>();
		CollectionUtils.convertStringToLong(fileHandleIds, fileHandleIdsLong);
		Set<Long> returnedFileHandleIds = nodeDao.getFileHandleIdsAssociatedWithFileEntity(fileHandleIdsLong, KeyFactory.stringToKey(entityId));
		Set<String> results = new HashSet<String>();
		CollectionUtils.convertLongToString(returnedFileHandleIds, results);
		return results;
	}

	@Override
	public List<EntityHeader> getChildren(String parentId,
			List<EntityType> includeTypes, Set<Long> childIdsToExclude,
			SortBy sortBy, Direction sortDirection, long limit, long offset) {
		// EntityManager handles all of the business logic for this call.
		return nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
	}

	@Override
	public String lookupChild(String parentId, String entityName) {
		// EntityManager handles all of the business logic for this call.
		return nodeDao.lookupChild(parentId, entityName);
	}
}
