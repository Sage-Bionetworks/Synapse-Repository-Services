package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeInheritanceManager {
	
	/**
	 * Get the benefactor of a node.
	 * @param nodeId
	 * @return
	 * @throws NotFoundException 
	 */
	public String getBenefactor(String nodeId) throws NotFoundException;

	/**
	 * This method should be called when a node's parent changes.
	 * 
	 * @param nodeId
	 */
	public void nodeParentChanged(String nodeId);
	
	/**
	 * This method is called when an Access Control List (ACL) is added to a node. When 
	 * this occurs a node no longer inherits its permission from a parent, instead it 
	 * inherits from itself.
	 * @param nodeId
	 * @throws NotFoundException 
	 */
	public void setNodeToInheritFromItself(String nodeId) throws NotFoundException;
	
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
	
}
