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
	String getBenefactor(String nodeId) throws NotFoundException, DatastoreException;

	/**
	 * When a node's parent has changed, sets the node and its descendants to the nearest benefactor.
	 * Nearest benefactor will be what the new parent node has as benefactor. The exception here is
	 * self-benefactors. If any node is a self-benefactor, its benefactor remains to be itself.
	 * (Note this method does not modify the node's parent ID.)
	 *
	 * @param nodeId The node whose parentId has changed
	 * @param parentNodeId The new parent node
	 *
	 * @see org.sagebionetworks.repo.model.NodeDAO#changeNodeParent(String, String)
	 */
	void nodeParentChanged(String nodeId, String parentNodeId) throws NotFoundException, DatastoreException;

	/**
	 * When a node's parent has changed, sets the node and its descendants to the nearest benefactor.
	 * Nearest benefactor will be what the new parent node has as benefactor.
	 * (Note this method does not modify the node's parent ID.)
	 *
	 * @param nodeId The node whose parentId has changed
	 * @param parentNodeId The new parent node
	 * @param skipSelfBenefactor Whether self-benefactors will be exempted from the change
	 *
	 * @see org.sagebionetworks.repo.model.NodeDAO#changeNodeParent(String, String)
	 */
	void nodeParentChanged(String nodeId, String parentNodeId, boolean skipSelfBenefactor) throws NotFoundException, DatastoreException;

	/**
	 * This method is called when an Access Control List (ACL) is added to a node. When 
	 * this occurs a node no longer inherits its permission from a parent, instead it 
	 * inherits from itself.
	 * @param nodeId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void setNodeToInheritFromItself(String nodeId) throws NotFoundException, DatastoreException;

	/**
	 * This method is called when an Access Control List (ACL) is removed from the given node.
	 * When this occurs the node will inherit its permissions from its nearest parent with 
	 * an ACL.
	 * 
	 * @param nodeId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void setNodeToInheritFromNearestParent(String nodeId) throws NotFoundException, DatastoreException;

	/**
	 * This method is called to set a nodes benefactor.
	 * @param beneficiaryId
	 * @param toBenefactorId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	void addBeneficiary(String beneficiaryId, String toBenefactorId) throws NotFoundException, DatastoreException;

}
