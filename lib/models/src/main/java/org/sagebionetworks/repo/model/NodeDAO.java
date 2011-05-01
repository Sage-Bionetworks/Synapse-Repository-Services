package org.sagebionetworks.repo.model;

import java.util.Set;


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
	 */
	public String createNew(Node node);
	
	/**
	 * Fetch a node using its id.
	 * @param id
	 * @return
	 */
	public Node getNode(String id);
	
	/**
	 * Delete a node using its id.
	 * @param id
	 */
	public void delete(String id);
	
	/**
	 * Get the Annotations for a node. 
	 * @param id
	 * @return
	 */
	public Annotations getAnnotations(String id);
	
	/**
	 * Get all of the children nodes of a given node.
	 * @param id
	 * @return
	 */
	public Set<Node> getChildren(String id);
	
	/**
	 * Fetch the eTag for a given node with the intentions of updating
	 * the node.  
	 * Note: It is likely that an implementation will start/join a transaction
	 * using "SELECT FOR UPDATE" to lock this node.  If this is the case
	 * then the caller will need to start/join the same transaction or
	 * the lock will be released when this call returns.
	 * @param id
	 * @return
	 */
	public Long getETagForUpdate(String id);
	
	/**
	 * Make changes to an existing node.
	 * @param updatedNode
	 */
	public void updateNode(Node updatedNode);
	
	/**
	 * Update a nodes annotations.
	 * @param id
	 * @param updatedAnnotations
	 */
	public void updateAnnotations(String id, Annotations updatedAnnotations);
	

}
