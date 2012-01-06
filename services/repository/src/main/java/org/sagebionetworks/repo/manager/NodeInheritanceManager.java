package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeInheritanceManager {
	
	/**
	 * Get the benefactor of a node.
	 * @param nodeId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String getBenefactor(String nodeId) throws NotFoundException, DatastoreException;

	/**
	 * Puts a Node's children into correct state for permissions/benefactorId
	 * when  node's parentId has changed.  This method works whether or not the
	 * parentId change has already been made.
	 * @param nodeId representing the node whose parentId has changed
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void nodeParentChanged(String nodeId, String parentNodeId) throws NotFoundException, DatastoreException;
	
	/**
	 * This method is called when an Access Control List (ACL) is added to a node. When 
	 * this occurs a node no longer inherits its permission from a parent, instead it 
	 * inherits from itself.
	 * @param nodeId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void setNodeToInheritFromItself(String nodeId) throws NotFoundException, DatastoreException;
	
	/**
	 * This method is called when an Access Control List (ACL) is removed from the given node.
	 * When this occurs the node will inherit its permissions from its nearest parent with 
	 * an ACL.
	 * 
	 * @param nodeId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void setNodeToInheritFromNearestParent(String nodeId) throws NotFoundException, DatastoreException;

	/**
	 * This method is called to set a nodes benefactor.
	 * @param beneficiaryId
	 * @param toBenefactorId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void addBeneficiary(String beneficiaryId, String toBenefactorId) throws NotFoundException, DatastoreException;
	
}
