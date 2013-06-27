package org.sagebionetworks.repo.manager;
  
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ReferenceDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.jdo.EntityNameValidation;
import org.sagebionetworks.repo.model.jdo.FieldTypeCache;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Sage business logic for node management.
 * @author jmhill
 *
 */
public class NodeManagerImpl implements NodeManager, InitializingBean {
	
	private static final String FILE_HANDLE_UNAUTHORIZED_TEMPLATE = "Only the creator of a FileHandle can assign it to an Entity.  FileHandleId = '%1$s', UserId = '%2$s'";

	static private Log log = LogFactory.getLog(NodeManagerImpl.class);	
	
	@Autowired
	NodeDAO nodeDao;	
	@Autowired
	AuthorizationManager authorizationManager;	
	@Autowired
	private AccessControlListDAO aclDAO;	
	@Autowired
	private EntityBootstrapper entityBootstrapper;	
	@Autowired
	private NodeInheritanceManager nodeInheritanceManager;	
	@Autowired
	private ReferenceDao referenceDao;
	@Autowired 
	private ActivityManager activityManager;
	
	
	/**
	 * This is used for unit test.
	 * @param nodeDao
	 * @param authDoa
	 */
	public NodeManagerImpl(NodeDAO nodeDao, AuthorizationManager authDoa, 
			AccessControlListDAO aclDao, EntityBootstrapper entityBootstrapper, 
			NodeInheritanceManager nodeInheritanceManager, ReferenceDao referenceDao, ActivityManager activityManager){
		this.nodeDao = nodeDao;
		this.authorizationManager = authDoa;
		this.aclDAO = aclDao;
		this.entityBootstrapper = entityBootstrapper;
		this.nodeInheritanceManager = nodeInheritanceManager;
		this.referenceDao = referenceDao;
		this.activityManager = activityManager;
	}
	
	/**
	 * Used by Spring
	 */
	public NodeManagerImpl(){
	}
	
	/**
	 * Create a new node
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNewNode(Node newNode, UserInfo userInfo)  throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException {
		// First valid the node
		NodeManagerImpl.validateNode(newNode);
		UserInfo.validateUserInfo(userInfo);
		// Also validate the username
		Long userIndividualGroupId = Long.parseLong(userInfo.getIndividualGroup().getId());
		// Validate the creations data
		NodeManagerImpl.validateNodeCreationData(userIndividualGroupId, newNode);
		// Validate the modified data.
		NodeManagerImpl.validateNodeModifiedData(userIndividualGroupId, newNode);
		
		// What is the object type of this node
		EntityType type = EntityType.valueOf(newNode.getNodeType());
		
		// By default all nodes inherit their their ACL from their parent.
		ACL_SCHEME aclSchem = ACL_SCHEME.INHERIT_FROM_PARENT;
		
		// If the user did not provide a parent then we use the default
		if(newNode.getParentId() == null){
			String defaultPath = type.getDefaultParentPath();
			if(defaultPath == null) throw new IllegalArgumentException("There is no default parent for Entities of type: "+type.name()+" so a valid parentId must be provided"); 
			// Get the parent node.
			String pathId = nodeDao.getNodeIdForPath(defaultPath);
			newNode.setParentId(pathId);
			// Lookup the acl scheme to be used for children of this parent
			aclSchem = entityBootstrapper.getChildAclSchemeForPath(defaultPath);
		}
		
		// check whether the user is allowed to create this type of node
		if (!authorizationManager.canCreate(userInfo, newNode)) {
			throw new UnauthorizedException(userInfo.getUser().getUserId()+" is not allowed to create items within container "+newNode.getParentId());
		}
		// Handle permission around file handles.
		if(newNode.getFileHandleId() != null){
			// To set the file handle on a create the caller must have permission 
			if(!authorizationManager.canAccessRawFileHandleById(userInfo, newNode.getFileHandleId())){
				throw new UnauthorizedException(createFileHandleUnauthorizedMessage(newNode.getFileHandleId(), userInfo));
			}
		}

		// check whether the user is allowed to connect to the specified activity
		canConnectToActivity(newNode.getActivityId(), userInfo);

		// If they are allowed then let them create the node
		String id = nodeDao.createNew(newNode);
		newNode.setId(id);
		
		// Setup the ACL for this node.
		if(ACL_SCHEME.INHERIT_FROM_PARENT == aclSchem){
			// This node inherits from its parent.
			String parentBenefactor = nodeInheritanceManager.getBenefactor(newNode.getParentId());
			nodeInheritanceManager.addBeneficiary(id, parentBenefactor);
		}else if(ACL_SCHEME.GRANT_CREATOR_ALL == aclSchem){
			AccessControlList rootAcl = AccessControlListUtil.createACLToGrantAll(id, userInfo);
			aclDAO.create(rootAcl);
			// This node is its own benefactor
			nodeInheritanceManager.addBeneficiary(id, id);
		}else{
			throw new IllegalArgumentException("Unknown ACL_SHEME: "+aclSchem);
		}
		
		// adding access is done at a higher level, not here
		//authorizationManager.addUserAccess(newNode, userInfo);
		if(log.isDebugEnabled()){
			log.debug("username: "+userInfo.getUser().getUserId()+" created node: "+id);
		}
		return id;
	}

	/**
	 * Create an unauthorized message for file handles.
	 * 
	 * @param fileHandleId
	 * @param userInfo
	 * @return
	 */
	private String createFileHandleUnauthorizedMessage(String fileHandleId,	UserInfo userInfo) {
		return String.format(FILE_HANDLE_UNAUTHORIZED_TEMPLATE, fileHandleId, userInfo.getIndividualGroup().getId());
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
		existingNode.clearNodeCreationData();
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// First validate the username
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException(userName+" lacks change access to the requested object.");
		}
		nodeDao.delete(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" deleted node: "+nodeId);
		}
		aclDAO.delete(nodeId);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteVersion(UserInfo userInfo, String id, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// First validate the username
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, id, ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException(userName+" lacks change access to the requested object.");
		}
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
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName + " lacks read access to the requested object.");
		}
		
		Node result = nodeDao.getNode(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" fetched node: "+result.getId());
		}
		return result;
	}

	@Override
	public Node getNodeForVersionNumber(UserInfo userInfo, String nodeId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		Node result = nodeDao.getNodeForVersion(nodeId, versionNumber);
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateForTrashCan(UserInfo userInfo, Node updatedNode, ChangeType changeType)
			throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		updateNode(userInfo, updatedNode, null, false, false, changeType);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Node update(UserInfo userInfo, Node updated)
			throws ConflictingUpdateException, NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		return update(userInfo, updated, null, false);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Node update(UserInfo userInfo, Node updatedNode, NamedAnnotations updatedAnnos, boolean newVersion)
			throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {

		UserInfo.validateUserInfo(userInfo);
		// Validate that the user can update the node.
		if (!authorizationManager.canAccess(userInfo, updatedNode.getId(), ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException(userInfo.getUser().getUserId() + " lacks change access to the requested object.");
		}
		
		// Validate that the user can assign the file handle if they have it.
		if(updatedNode.getFileHandleId() != null){
			// First determine if this is a change
			String currentHandleId = nodeDao.getFileHandleIdForCurrentVersion(updatedNode.getId());
			if(!updatedNode.getFileHandleId().equals(currentHandleId)){
				// This is a change so the user must be the creator of the new file handle
				if(!authorizationManager.canAccessRawFileHandleById(userInfo, updatedNode.getFileHandleId())){
					throw new UnauthorizedException(createFileHandleUnauthorizedMessage(updatedNode.getFileHandleId(), userInfo));
				}
			}
		}
		updateNode(userInfo, updatedNode, updatedAnnos, newVersion, true, ChangeType.UPDATE);

		return get(userInfo, updatedNode.getId());
	}

	private void updateNode(UserInfo userInfo, Node updatedNode, NamedAnnotations updatedAnnos,
			boolean newVersion, boolean skipBenefactor, ChangeType changeType)
			throws ConflictingUpdateException, NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {

		NodeManagerImpl.validateNode(updatedNode);

		// Make sure the eTags match
		if (updatedAnnos != null) {
			if (!updatedNode.getETag().equals(updatedAnnos.getEtag())) {
				throw new IllegalArgumentException("The passed node and annotations do not have the same eTag");
			}
		}

		canConnectToActivity(updatedNode.getActivityId(), userInfo);

		final String nextETag = nodeDao.lockNodeAndIncrementEtag(updatedNode.getId(), updatedNode.getETag(), changeType);

		// Clear node creation data to make sure NodeDAO does not change the fields
		NodeManagerImpl.clearNodeCreationDataForUpdate(updatedNode);

		// Clear the modified data and fill it in with the new data
		Long userIndividualGroupId = Long.parseLong(userInfo.getIndividualGroup().getId());
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
		final String parentInDatabase = nodeDao.getParentId(updatedNode.getId());
		final String parentInUpdate = updatedNode.getParentId();
		final String nodeInUpdate = updatedNode.getId();
		if (isParenIdChange(parentInDatabase, parentInUpdate)) {
			if (!authorizationManager.canAccess(userInfo, parentInUpdate, ACCESS_TYPE.CREATE)) {
				throw new UnauthorizedException(userInfo.getUser().getDisplayName() + " is not authorized to create an entity here.");
			}
			nodeDao.changeNodeParent(nodeInUpdate, parentInUpdate);
			// Update the ACL accordingly
			if (skipBenefactor) {
				nodeInheritanceManager.nodeParentChanged(nodeInUpdate, parentInUpdate);
			} else {
				// If the node is being moved to right under the root, we need to create a new ACL
				// The root cannot be the benefactor
				boolean newAcl = nodeDao.isNodeRoot(parentInUpdate);
				EntityType type = EntityType.valueOf(updatedNode.getNodeType());
				String defaultPath = type.getDefaultParentPath();
				ACL_SCHEME aclSchem = entityBootstrapper.getChildAclSchemeForPath(defaultPath);
				newAcl = newAcl && (ACL_SCHEME.GRANT_CREATOR_ALL == aclSchem);
				if (newAcl) {
					AccessControlList acl = AccessControlListUtil.createACLToGrantAll(nodeInUpdate, userInfo);
					aclDAO.create(acl);
					nodeInheritanceManager.setNodeToInheritFromItself(nodeInUpdate, false);
				} else {
					// Remove the ACLs of all the benefactors
					removeBenefactorAcl(nodeInUpdate);
					// Set the new parent as the benefactor
					nodeInheritanceManager.nodeParentChanged(nodeInUpdate, parentInUpdate, false);
				}
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
			log.debug("username "+userInfo.getUser().getUserId()+" updated node: "+updatedNode.getId()+", with a new eTag: "+nextETag);
		}
	}

	/**
	 * Is this a parent ID change.  Note: ParenID can be null.
	 * This was added for PLFM-1533.
	 * @param one
	 * @param two
	 * @return
	 */
	public static boolean isParenIdChange(String one, String two){
		if(one == null){
			if(two != null){
				return true;
			}else{
				return false;
			}
		}else{
			return !one.equals(two);
		}
	}

	@Override
	public NamedAnnotations getAnnotations(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
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
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		return nodeDao.getAnnotationsForVersion(nodeId, versionNumber);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Annotations updateAnnotations(UserInfo userInfo, String nodeId, Annotations updated, String namespace) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updated == null) throw new IllegalArgumentException("Annotations cannot be null");
		if(nodeId == null) throw new IllegalArgumentException("Node ID cannot be null");
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		// This is no longer called from a create PLFM-325
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException(userName+" lacks update access to the requested object.");
		}
		// Validate that the annotations
		validateAnnotations(updated);
		// Now lock the node if we can
		nodeDao.lockNodeAndIncrementEtag(nodeId, updated.getEtag());
		NamedAnnotations namedAnnos = nodeDao.getAnnotations(nodeId);
		// Replace a single namespace
		namedAnnos.put(namespace, updated);
		
		nodeDao.updateAnnotations(nodeId, namedAnnos);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" updated Annotations for node: "+updated.getId());
		}
		namedAnnos = getAnnotations(userInfo, nodeId);
		return namedAnnos.getAnnotationsForName(namespace);
	}
	
	public void validateAnnotations(NamedAnnotations updatedAnnos) throws DatastoreException, InvalidModelException {
		if(updatedAnnos == null) throw new IllegalArgumentException("NamedAnnotations cannot be null");
		// Validate all of the annotations
		Iterator<String> it = updatedAnnos.nameIterator();
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
		FieldTypeCache.validateAnnotations(updated);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// This is a hack because the current DAO is not working with integration tests.
//		authorizationManager = new TempMockAuthDao();
	}

	@Override
	public Set<Node> getChildren(UserInfo userInfo, String parentId) throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, parentId, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		return nodeDao.getChildren(parentId);
	}

	@Override
	public EntityType getNodeType(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		Node node = get(userInfo, nodeId);
		return EntityType.valueOf(node.getNodeType());
	}
	
	@Override
	public List<Long> getAllVersionNumbersForNode(UserInfo userInfo,
			String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate that the user can do what they are trying to do.
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		// If they are allowed to read a node then get the list.
		return nodeDao.getVersionNumbers(nodeId);
	}

	@Override
	public List<EntityHeader> getNodePath(UserInfo userInfo, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		return nodeDao.getEntityPath(nodeId);
	}

	@Override
	public List<EntityHeader> getNodePathAsAdmin(String nodeId)	throws NotFoundException, DatastoreException {
		// This version does not require authorization.
		return nodeDao.getEntityPath(nodeId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNewNode(Node newNode, NamedAnnotations newAnnotations, UserInfo userInfo) throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException {
		// First create the node
		String id = createNewNode(newNode, userInfo);
		// The eTag really has no meaning yet because nobody has access to this id until we return.
		newAnnotations.setEtag(id);
		newAnnotations.setId(id);
		validateAnnotations(newAnnotations);
		// Since we just created this node we do not need to lock.
		nodeDao.updateAnnotations(id, newAnnotations);
		return id;
	}

	@Override
	public EntityHeader getNodeHeader(UserInfo userInfo, String entityId, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		return nodeDao.getEntityHeader(entityId, versionNumber);
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
		List<EntityHeader> results = new ArrayList<EntityHeader>(entityHeaderList.size());
		for (EntityHeader entityHeader: entityHeaderList) {
			if (authorizationManager.canAccess(userInfo, entityHeader.getId(), ACCESS_TYPE.READ)) {
				results.add(entityHeader);
			}
		}
		return results;
	}

	@Override
	public QueryResults<EntityHeader> getEntityReferences(UserInfo userInfo, String nodeId, Integer versionNumber, Integer offset, Integer limit)
			throws NotFoundException, DatastoreException {
		UserInfo.validateUserInfo(userInfo);
		return referenceDao.getReferrers(KeyFactory.stringToKey(nodeId), versionNumber, userInfo, offset, limit);
	}

	@Override
	public boolean doesNodeHaveChildren(String nodeId) {
		return nodeDao.doesNodeHaveChildren(nodeId);
	}

	@Override
	public QueryResults<VersionInfo> getVersionsOfEntity(UserInfo userInfo,
			String entityId, long offset, long limit) throws NotFoundException,
			UnauthorizedException, DatastoreException {
		validateReadAccess(userInfo, entityId);
		return nodeDao.getVersionsOfEntity(entityId, offset, limit);
	}

	public void validateReadAccess(UserInfo userInfo, String entityId)
			throws NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		if (!authorizationManager.canAccess(userInfo, entityId, ACCESS_TYPE.READ)) {
			String userName = userInfo.getUser().getUserId();
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setActivityForNode(UserInfo userInfo, String nodeId,
			String activityId) throws NotFoundException, UnauthorizedException,
			DatastoreException {
		Node toUpdate = get(userInfo, nodeId);		
		toUpdate.setActivityId(activityId);
		update(userInfo, toUpdate);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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
			if(!authorizationManager.canAccessActivity(userInfo, activityId))
				throw new UnauthorizedException(userInfo.getUser().getUserId() +" lacks change access to the specified activity object.");
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public VersionInfo promoteEntityVersion(UserInfo userInfo, String nodeId, Long versionNumber)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException(userInfo.getUser().getUserId() +" lacks change access to " + nodeId + ".");
		}
		Long currentVersion = nodeDao.getCurrentRevisionNumber(nodeId);
		if (!currentVersion.equals(versionNumber)) {
			String currentETag = nodeDao.peekCurrentEtag(nodeId);
			nodeDao.lockNodeAndIncrementEtag(nodeId, currentETag);
			Node nodeToPromote = nodeDao.getNodeForVersion(nodeId, versionNumber);
			nodeToPromote.setVersionLabel(null); // To get a new version label
			nodeDao.createNewVersion(nodeToPromote);
		}
		QueryResults<VersionInfo> versions = nodeDao.getVersionsOfEntity(nodeId, 0, 1);
		return versions.getResults().get(0);
	}

	@Override
	public String getFileHandleIdForCurrentVersion(UserInfo userInfo, String id) throws NotFoundException, UnauthorizedException {
		// Validate that the user has download permission.
		validateDownload(userInfo, id);
		// Get the value from the dao
		String fileHandleId = nodeDao.getFileHandleIdForCurrentVersion(id);
		checkFileHandleId(id, fileHandleId);
		return fileHandleId;
	}

	@Override
	public String getFileHandleIdForVersion(UserInfo userInfo, String id, Long versionNumber) throws NotFoundException, UnauthorizedException {
		// Validate that the user has download permission.
		validateDownload(userInfo, id);
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

	/**
	 * Helper to validate that the user has download permission.
	 * @param userInfo
	 * @param id
	 * @throws NotFoundException
	 */
	private void validateDownload(UserInfo userInfo, String id)
			throws NotFoundException {
		// First the user must have read access to the entity.
		validateReadAccess(userInfo, id);
		// They must have permission to dowload the file to get the handle.
		if (!authorizationManager.canAccess(userInfo, id, ACCESS_TYPE.DOWNLOAD)) {
			throw new UnauthorizedException(userInfo.getUser().getUserId() +" lacks download access to the specified object.");
		}
	}

	/**
	 * Recursively remove the ACLs of all the descendants who are benefactors.
	 */
	private void removeBenefactorAcl(String nodeId)  {
		String benefactor = null;
		try {
			benefactor = nodeInheritanceManager.getBenefactor(nodeId);
		} catch (NotFoundException e) {
			benefactor = null;
		}
		if (nodeId.equals(benefactor)) {
			try {
				aclDAO.delete(nodeId);
			} catch (NotFoundException e) {
				throw new DatastoreException("ACL for benefactor " + nodeId + " is not found");
			}
		}
		List<String> children = nodeDao.getChildrenIdsAsList(nodeId);
		if (children == null || children.size() == 0) {
			return;
		}
		for (String child : children) {
			removeBenefactorAcl(child);
		}
	}
}
