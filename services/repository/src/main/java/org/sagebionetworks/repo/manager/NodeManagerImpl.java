package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.jdo.EntityNameValidation;
import org.sagebionetworks.repo.model.query.FieldType;
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
@Transactional(readOnly = true)
public class NodeManagerImpl implements NodeManager, InitializingBean {
	
	static private Log log = LogFactory.getLog(NodeManagerImpl.class);
	
	@Autowired
	NodeDAO nodeDao;
	
	@Autowired
	AuthorizationManager authorizationManager;
	
	@Autowired
	FieldTypeDAO fieldTypeDao;
	
	@Autowired
	private AccessControlListDAO aclDAO;
	
	@Autowired
	private EntityBootstrapper entityBootstrapper;
	
	@Autowired
	private NodeInheritanceDAO inheritanceDAO;
	
	// for testing (in prod it's autowired)
	public void setAuthorizationManager(AuthorizationManager authorizationManager) {
		 this.authorizationManager =  authorizationManager;
	}
	
	/**
	 * This is used for unit test.
	 * @param nodeDao
	 * @param authDoa
	 */
	public NodeManagerImpl(NodeDAO nodeDao, AuthorizationManager authDoa, FieldTypeDAO fieldTypeday, AccessControlListDAO aclDao, EntityBootstrapper entityBootstrapper, NodeInheritanceDAO inheritanceDAO){
		this.nodeDao = nodeDao;
		this.authorizationManager = authDoa;
		this.fieldTypeDao = fieldTypeday;
		this.aclDAO = aclDao;
		this.entityBootstrapper = entityBootstrapper;
		this.inheritanceDAO = inheritanceDAO;
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
		String userName = userInfo.getUser().getUserId();
		// Validate the creations data
		NodeManagerImpl.validateNodeCreationData(userName, newNode);
		// Validate the modified data.
		NodeManagerImpl.validateNodeModifiedData(userName, newNode);
		
		// What is the object type of this node
		ObjectType type = ObjectType.valueOf(newNode.getNodeType());
		
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
			throw new UnauthorizedException(userName+" is not allowed to create items of type "+newNode.getNodeType());
		}

		// If they are allowed then let them create the node
		String id = nodeDao.createNew(newNode);
		newNode.setId(id);
		
		// Setup the ACL for this node.
		if(ACL_SCHEME.INHERIT_FROM_PARENT == aclSchem){
			// This node inherits from its parent.
			String parentBenefactor = inheritanceDAO.getBenefactor(newNode.getParentId());
			inheritanceDAO.addBeneficiary(id, parentBenefactor);
		}else if(ACL_SCHEME.GRANT_CREATOR_ALL == aclSchem){
			AccessControlList rootAcl = AccessControlList.createACLToGrantAll(id, userInfo);
			aclDAO.create(rootAcl);
			// This node is its own benefactor
			inheritanceDAO.addBeneficiary(id, id);
		}else{
			throw new IllegalArgumentException("Unknown ACL_SHEME: "+aclSchem);
		}
		
		// adding access is done at a higher level, not here
		//authorizationManager.addUserAccess(newNode, userInfo);
		if(log.isDebugEnabled()){
			log.debug("username: "+userName+" created node: "+id);
		}
		return id;
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
	public static void validateNodeCreationData(String userName, Node newNode){
		if(userName == null) throw new IllegalArgumentException("Username cannot be null");
		if(newNode == null) throw new IllegalArgumentException("New node cannot be null");
		// If createdBy is not set then set it
		if(newNode.getCreatedBy() == null ){
			newNode.setCreatedBy(userName);
		}
		// If createdOn is not set then set it with the current time.
		if(newNode.getCreatedOn() == null){
			newNode.setCreatedOn(new Date(System.currentTimeMillis()));
		}
	}
	
	/**
	 * Make sure the creation data is set, and if not then set it.
	 * @param userName
	 * @param newNode
	 * @return
	 */
	public static void validateNodeModifiedData(String userName, Node newNode){
		if(userName == null) throw new IllegalArgumentException("Username cannot be null");
		if(newNode == null) throw new IllegalArgumentException("New node cannot be null");
		// If createdBy is not set then set it
		newNode.setModifiedBy(userName);
		newNode.setModifiedOn(new Date(System.currentTimeMillis()));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// First validate the username
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, AuthorizationConstants.ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException(userName+" lacks change access to the requested object.");
		}
		nodeDao.delete(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" deleted node: "+nodeId);
		}
		
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteVersion(UserInfo userInfo, String id, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// First validate the username
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, id, AuthorizationConstants.ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException(userName+" lacks change access to the requested object.");
		}
		// Lock before we delete
		String currentETag = nodeDao.peekCurrentEtag(id);
		nodeDao.lockNodeAndIncrementEtag(id, currentETag);
		// Delete while holding the lock.
		nodeDao.deleteVersion(id, versionNumber);
	}
	
	@Transactional(readOnly = true)
	@Override
	public Node get(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate the username
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, AuthorizationConstants.ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		
		Node result = nodeDao.getNode(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" fetched node: "+result.getId());
		}
		return result;
	}
	
	@Transactional(readOnly = true)
	@Override
	public Node getNodeForVersionNumber(UserInfo userInfo, String nodeId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, AuthorizationConstants.ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		Node result = nodeDao.getNodeForVersion(nodeId, versionNumber);
		return result;
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
	public Node update(UserInfo userInfo, Node updatedNode, Annotations updatedAnnos, boolean newVersion) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		NodeManagerImpl.validateNode(updatedNode);
		if (!authorizationManager.canAccess(userInfo, updatedNode.getId(), AuthorizationConstants.ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException(userName+" lacks change access to the requested object.");
		}
		// make sure the eTags match
		if(updatedAnnos != null){
			if(!updatedNode.getETag().equals(updatedAnnos.getEtag())) throw new IllegalArgumentException("The passed node and annotations do not have the same eTag");
		}
		// Now lock this node
		String nextETag = nodeDao.lockNodeAndIncrementEtag(updatedNode.getId(), updatedNode.getETag());
		
		// Clear the modified data and fill it in with the new data
		NodeManagerImpl.validateNodeModifiedData(userName, updatedNode);
		// If this is a new version then we need to create a new version before the update
		if(newVersion){
			// This will create a new version and set the new version to 
			// be the current version.  Then the rest of the update will then
			// be applied to this new version.
			nodeDao.createNewVersion(updatedNode);
		}
		
		// Now make the actual update.
		nodeDao.updateNode(updatedNode);
		// Also update the Annotations if provided
		if(updatedAnnos != null){
			updatedAnnos.setEtag(nextETag);
			validateAnnotations(updatedAnnos);
			nodeDao.updateAnnotations(updatedNode.getId(), updatedAnnos);
		}
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" updated node: "+updatedNode.getId()+", with a new eTag: "+nextETag);
		}
		// Return the new node
		return get(userInfo, updatedNode.getId());
	}

	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotations(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, AuthorizationConstants.ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		Annotations annos = nodeDao.getAnnotations(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" fetched Annotations for node: "+nodeId);
		}
		return annos;
	}
	
	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotationsForVersion(UserInfo userInfo, String nodeId, Long versionNumber) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, AuthorizationConstants.ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		return nodeDao.getAnnotationsForVersion(nodeId, versionNumber);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Annotations updateAnnotations(UserInfo userInfo, String nodeId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updated == null) throw new IllegalArgumentException("Annotations cannot be null");
		if(nodeId == null) throw new IllegalArgumentException("Node ID cannot be null");
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		// This is no longer called from a create PLFM-325
		if (!authorizationManager.canAccess(userInfo, nodeId, AuthorizationConstants.ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException(userName+" lacks update access to the requested object.");
		}
		// Validate that the annotations
		validateAnnotations(updated);
		// Now lock the node if we can
		nodeDao.lockNodeAndIncrementEtag(nodeId, updated.getEtag());
		nodeDao.updateAnnotations(nodeId, updated);
		if(log.isDebugEnabled()){
			log.debug("username "+userName+" updated Annotations for node: "+updated.getId());
		}
		return getAnnotations(userInfo, nodeId);
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
		
		// Validate the strings
		if(updated.getStringAnnotations() != null){
			Iterator<String> it = updated.getStringAnnotations().keySet().iterator();
			while(it.hasNext()){
				fieldTypeDao.addNewType(it.next(), FieldType.STRING_ATTRIBUTE);
			}
		}
		// Validate the longs
		if(updated.getLongAnnotations() != null){
			Iterator<String> it = updated.getLongAnnotations().keySet().iterator();
			while(it.hasNext()){
				fieldTypeDao.addNewType(it.next(), FieldType.LONG_ATTRIBUTE);
			}
		}
		// Validate the dates
		if(updated.getDateAnnotations() != null){
			Iterator<String> it = updated.getDateAnnotations().keySet().iterator();
			while(it.hasNext()){
				fieldTypeDao.addNewType(it.next(), FieldType.DATE_ATTRIBUTE);
			}
		}
		// Validate the Doubles
		if(updated.getDoubleAnnotations() != null){
			Iterator<String> it = updated.getDoubleAnnotations().keySet().iterator();
			while(it.hasNext()){
				fieldTypeDao.addNewType(it.next(), FieldType.DOUBLE_ATTRIBUTE);
			}
		}
		// Validate the Doubles
		if(updated.getBlobAnnotations() != null){
			Iterator<String> it = updated.getBlobAnnotations().keySet().iterator();
			while(it.hasNext()){
				fieldTypeDao.addNewType(it.next(), FieldType.BLOB_ATTRIBUTE);
			}
		}
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
		if (!authorizationManager.canAccess(userInfo, parentId, AuthorizationConstants.ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		return nodeDao.getChildren(parentId);
	}

	@Transactional(readOnly = true)
	@Override
	public ObjectType getNodeType(UserInfo userInfo, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		Node node = get(userInfo, nodeId);
		return ObjectType.valueOf(node.getNodeType());
	}

	@Transactional(readOnly = true)
	@Override
	public List<Long> getAllVersionNumbersForNode(UserInfo userInfo,
			String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate that the user can do what they are trying to do.
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, AuthorizationConstants.ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		// If they are allowed to read a node then get the list.
		return nodeDao.getVersionNumbers(nodeId);
	}

	@Transactional(readOnly = true)
	@Override
	public List<EntityHeader> getNodePath(UserInfo userInfo, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo.validateUserInfo(userInfo);
		String userName = userInfo.getUser().getUserId();
		if (!authorizationManager.canAccess(userInfo, nodeId, AuthorizationConstants.ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userName+" lacks read access to the requested object.");
		}
		return nodeDao.getEntityPath(nodeId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNewNode(Node newNode, Annotations newAnnotations, UserInfo userInfo) throws DatastoreException,
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

}
