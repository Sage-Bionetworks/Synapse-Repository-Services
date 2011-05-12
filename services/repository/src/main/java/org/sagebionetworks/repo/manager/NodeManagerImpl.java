package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
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
	
//	public static final String ANNONYMOUS = "anonymous";
	
	@Autowired
	NodeDAO nodeDao;
	
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	FieldTypeDAO fieldTypeDao;
	
	// for testing (in prod it's autowired)
	public void setAuthorizationManager(AuthorizationManager authorizationManager) {
		 this.authorizationManager =  authorizationManager;
	}
	
	/**
	 * This is used for unit test.
	 * @param nodeDao
	 * @param authDoa
	 */
	public NodeManagerImpl(NodeDAO nodeDao, AuthorizationManager authDoa, FieldTypeDAO fieldTypeday){
		this.nodeDao = nodeDao;
		this.authorizationManager = authDoa;
		this.fieldTypeDao = fieldTypeday;
	}
	
	/**
	 * Used by Spring
	 */
	public NodeManagerImpl(){
	}
	
	/**
	 * Note: Cannot do authorization here, since it is object specific and "Node" is generic.
	 * Authorization must be done at the layer that calls this one.
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNewNode(Node newNode, String userName)  throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException {
		// First valid the node
		NodeManagerImpl.validateNode(newNode);
		// Also validate the username
		userName  = NodeManagerImpl.validateUsername(userName);
		// Validate the creations data
		NodeManagerImpl.validateNodeCreationData(userName, newNode);
		// Validate the modified data.
		NodeManagerImpl.validateNodeModifiedData(userName, newNode);
		// check whether the user is allowed to create this type of node
		if (!authorizationManager.canCreate(userName, newNode.getNodeType())) {
			throw new UnauthorizedException(userName+" is not allowed to create items of type "+newNode.getNodeType());
		}

		// If they are allowed then let them create the node
		String id = nodeDao.createNew(newNode);
		newNode.setId(id);
		authorizationManager.addUserAccess(newNode, userName);
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
		if(node.getName() == null) throw new IllegalArgumentException("Node.name cannot be null");		
	}
	
	/**
	 * Validate the passed user name.
	 * @param userName
	 * @return
	 */
	public static String validateUsername(String userName){
		if(userName == null || "".equals(userName.trim())){
			return AuthUtilConstants.ANONYMOUS_USER_ID;
		}else{
			return userName.trim();
		}
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
	public void delete(String username, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// First validate the username
		username = NodeManagerImpl.validateUsername(username);
		if (!authorizationManager.canAccess(username, nodeId, AuthorizationConstants.ACCESS_TYPE.CHANGE)) {
			throw new UnauthorizedException(username+" lacks change access to the requested object.");
		}

		
		nodeDao.delete(nodeId);
		authorizationManager.removeAuthorization(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+username+" deleted node: "+nodeId);
		}
		
	}
	
	@Transactional(readOnly = true)
	@Override
	public Node get(String username, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate the username
		username = NodeManagerImpl.validateUsername(username);
		if (!authorizationManager.canAccess(username, nodeId, AuthorizationConstants.ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(username+" lacks read access to the requested object.");
		}
		
		Node result = nodeDao.getNode(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+username+" fetched node: "+result.getId());
		}
		return result;
	}
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Node update(String userName, Node updated)
			throws ConflictingUpdateException, NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		return update(userName, updated, null);
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Node update(String username, Node updatedNode, Annotations updatedAnnos) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		username = NodeManagerImpl.validateUsername(username);
		NodeManagerImpl.validateNode(updatedNode);
		if (!authorizationManager.canAccess(username, updatedNode.getId(), AuthorizationConstants.ACCESS_TYPE.CHANGE)) {
			throw new UnauthorizedException(username+" lacks change access to the requested object.");
		}
		// make sure the eTags match
		if(updatedAnnos != null){
			if(!updatedNode.getETag().equals(updatedAnnos.getEtag())) throw new IllegalArgumentException("The passed node and annotations do not have the same eTag");
//			if(!updatedNode.getId().equals(updatedAnnos.getId())) throw new IllegalArgumentException("The passed node and annotations do not have the same ID");
		}
		// Now lock this node
		String nextETag = validateETagAndLockNode(updatedNode.getId(), updatedNode.getETag());
		// We have the lock
		// Increment the eTag
		updatedNode.setETag(nextETag);
		
		// Clear the modified data and fill it in with the new data
		NodeManagerImpl.validateNodeModifiedData(username, updatedNode);
		// Now make the actual update.
		nodeDao.updateNode(updatedNode);
		// Also update the Annotations if provided
		if(updatedAnnos != null){
			updatedAnnos.setEtag(nextETag);
			validateAnnotations(updatedAnnos);
			nodeDao.updateAnnotations(updatedNode.getId(), updatedAnnos);
		}
		if(log.isDebugEnabled()){
			log.debug("username "+username+" updated node: "+updatedNode.getId()+", with a new eTag: "+nextETag);
		}
		// Return the new node
		return updatedNode;
	}
	
	/**
	 * Note: This must be called from within a Transaction.
	 * Calling this method will validate the passed eTag against the current eTag for the given node.
	 * A lock will also me maintained on this node until the transaction either rolls back or commits.
	 * 
	 * Note: This is a blocking call.  If another transaction is currently holding the lock on this node
	 * this method will be blocked, until the lock is released.
	 * 
	 * @param nodeId
	 * @param eTag
	 * @throws ConflictingUpdateException
	 * @throws NotFoundException 
	 */
	protected String validateETagAndLockNode(String nodeId, String eTag) throws ConflictingUpdateException, NotFoundException{
		if(eTag == null) throw new IllegalArgumentException("Must have a non-null eTag to update a node");
		if(nodeId == null) throw new IllegalArgumentException("Must have a non-null ID to update a node");
		long passedTag = Long.parseLong(eTag);
		// Get the etag
		long currentTag = nodeDao.getETagForUpdate(nodeId);
		if(passedTag != currentTag){
			throw new ConflictingUpdateException("Node: "+nodeId+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Increment the eTag
		currentTag++;
		return new Long(currentTag).toString();
	}

	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	@Override
	public boolean hasAccess(Node resource, AuthorizationConstants.ACCESS_TYPE accessType, String userName) throws NotFoundException, DatastoreException  {
		return authorizationManager.canAccess(userName, resource.getId(), accessType);
	}

	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotations(String username, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		username = NodeManagerImpl.validateUsername(username);
		Annotations annos = nodeDao.getAnnotations(nodeId);
		if(log.isDebugEnabled()){
			log.debug("username "+username+" fetched Annotations for node: "+nodeId);
		}
		return annos;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Annotations updateAnnotations(String username, String nodeId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updated == null) throw new IllegalArgumentException("Annotations cannot be null");
		if(nodeId == null) throw new IllegalArgumentException("Node ID cannot be null");
		username = NodeManagerImpl.validateUsername(username);
		// Validate that the annotations
		validateAnnotations(updated);
		// Now lock the node if we can
		String nextETag = validateETagAndLockNode(nodeId, updated.getEtag());
		// We have the lock
		// Increment the eTag
		updated.setEtag(nextETag);
		nodeDao.updateAnnotations(nodeId, updated);
		if(log.isDebugEnabled()){
			log.debug("username "+username+" updated Annotations for node: "+updated.getId());
		}
		return updated;
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
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// This is a hack because the current DAO is not working with integration tests.
		authorizationManager = new TempMockAuthDao();
	}

	@Override
	public Set<Node> getChildren(String userId, String parentId) throws NotFoundException {
		return nodeDao.getChildren(parentId);
	}

}
