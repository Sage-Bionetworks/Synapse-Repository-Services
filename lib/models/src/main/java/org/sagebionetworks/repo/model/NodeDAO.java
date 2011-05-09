package org.sagebionetworks.repo.model;

import java.util.Set;

import org.sagebionetworks.repo.web.NotFoundException;


/**
 * Interface for all Node C.R.U.D. operations.
 * 
 * @author jmhill
 *
 */
public interface NodeDAO {
	
	/**
	 * Create a new node.
	 * @param parentId can be null, or an existing parent node, to add this node to.
	 * @param node
	 * @return
	 * @throws NotFoundException 
	 * @throws NumberFormatException 
	 */
	public String createNew(Node node) throws NotFoundException;
	
	/**
	 * Fetch a node using its id.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public Node getNode(String id) throws NotFoundException;
	
	/**
	 * Delete a node using its id.
	 * @param id
	 * @throws NotFoundException 
	 */
	public void delete(String id) throws NotFoundException;
	
	/**
	 * Get the Annotations for a node. 
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public Annotations getAnnotations(String id) throws NotFoundException;
	
	/**
	 * Get all of the children nodes of a given node.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public Set<Node> getChildren(String id) throws NotFoundException;
	
	/**
	 * Fetch the eTag for a given node with the intentions of updating
	 * the node.  
	 * Note: It is likely that an implementation will start/join a transaction
	 * using "SELECT FOR UPDATE" to lock this node.  If this is the case
	 * then the caller will need to start/join the same transaction or
	 * the lock will be released when this call returns.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 */
	public Long getETagForUpdate(String id) throws NotFoundException;
	
	/**
	 * Make changes to an existing node.
	 * @param updatedNode
	 * @throws NotFoundException 
	 */
	public void updateNode(Node updatedNode) throws NotFoundException;
	
	/**
	 * Update a nodes annotations.
	 * @param id
	 * @param updatedAnnotations
	 * @throws NotFoundException 
	 */
	public void updateAnnotations(String nodeId, Annotations updatedAnnotations) throws NotFoundException;
	

}
