package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.logging.Logger;

import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Sage business logic for node management.
 * @author jmhill
 *
 */
@Transactional(readOnly = true)
public class NodeManagerImpl implements NodeManager {
	
	private static final Logger log = Logger.getLogger(NodeManagerImpl.class.getName());
	
	public static final String ANNONYMOUS = "anonymous";
	
	@Autowired
	NodeDAO nodeDao;
	@Autowired
	AuthorizationDAO authorizationDao;
	
	/**
	 * This is used for unit test.
	 * @param nodeDao
	 * @param authDoa
	 */
	public NodeManagerImpl(NodeDAO nodeDao, AuthorizationDAO authDoa){
		this.nodeDao = nodeDao;
		this.authorizationDao = authDoa;
	}
	
	/**
	 * Used by Spring
	 */
	public NodeManagerImpl(){
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createNewNode(String userName, Node newNode) {
		// First valid the node
		NodeManagerImpl.validateNode(newNode);
		// Also validate the username
		userName  = NodeManagerImpl.validateUsername(userName);
		// Validate the creations data
		NodeManagerImpl.validateNodeCreationData(userName, newNode);
		// Validate the modified data.
		NodeManagerImpl.validateNodeModifiedData(userName, newNode);
		// TODO: Add authz code
		
		// If they are allowed then let them create the node
		String id = nodeDao.createNew(newNode);
		log.info("username: "+userName+" created node: "+id);
		return id;
	}
	
	/**
	 * Validate a node
	 * @param userName
	 * @param node
	 */
	public static void validateNode(Node node){
		if(node == null) throw new IllegalArgumentException("Node cannot be null");
		if(node.getType() == null) throw new IllegalArgumentException("Node.type cannot be null");
		if(node.getName() == null) throw new IllegalArgumentException("Node.name cannot be null");		
	}
	
	/**
	 * Validate the passed user name.
	 * @param userName
	 * @return
	 */
	public static String validateUsername(String userName){
		if(userName == null || "".equals(userName.trim())){
			return ANNONYMOUS;
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
		if(newNode.getModifiedBy() == null ){
			newNode.setModifiedBy(userName);
		}
		// If createdOn is not set then set it with the current time.
		if(newNode.getModifiedOn() == null){
			newNode.setModifiedOn(new Date(System.currentTimeMillis()));
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String username, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if (!authorizationDao.canAccess(username, nodeId, AuthorizationDAO.CHANGE_ACCESS)) {
			throw new UnauthorizedException(username+" lacks change access to the requested object.");
		}
		// First validate the username
		username = NodeManagerImpl.validateUsername(username);
		// Add authz
		
		nodeDao.delete(nodeId);
		log.info("username "+username+" deleted node: "+nodeId);
		
	}
	
	@Transactional(readOnly = true)
	@Override
	public Node get(String username, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if (!authorizationDao.canAccess(username, nodeId, AuthorizationDAO.READ_ACCESS)) {
			throw new UnauthorizedException(username+" lacks read access to the requested object.");
		}
		// Validate the username
		username = NodeManagerImpl.validateUsername(username);
		// TODO: add authz
		
		Node result = nodeDao.getNode(nodeId);
		log.info("username "+username+" fetched node: "+result.getId());
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Node update(String username, Node updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException {
		if (!authorizationDao.canAccess(username, updated.getId(), AuthorizationDAO.CHANGE_ACCESS)) {
			throw new UnauthorizedException(username+" lacks change access to the requested object.");
		}
		username = NodeManagerImpl.validateUsername(username);
		NodeManagerImpl.validateNode(updated);
		// TODO: Authorization should occur before we lock
		
		
		// Now lock this node
		String nextETag = validateETagAndLockNode(updated.getId(), updated.geteTag());
		// We have the lock
		// Increment the eTag
		updated.seteTag(nextETag);
		
		// Clear the modified data and fill it in with the new data
		updated.setModifiedBy(null);
		updated.setModifiedOn(null);
		NodeManagerImpl.validateNodeModifiedData(username, updated);
		// Now make the actual update.
		nodeDao.updateNode(updated);
		log.info("username "+username+" updated node: "+updated.getId()+", with a new eTag: "+nextETag);
		// Return the new node
		return updated;
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
	 */
	protected String validateETagAndLockNode(String nodeId, String eTag) throws ConflictingUpdateException{
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
	public boolean hasAccess(Node resource, String accessType, String userName) throws NotFoundException, DatastoreException  {
		return true;
//		return authorizationDao.canAccess(userName, resource.getId(), accessType);
	}

}
