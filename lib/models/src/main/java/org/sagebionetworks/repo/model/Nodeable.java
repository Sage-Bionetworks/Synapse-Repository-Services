package org.sagebionetworks.repo.model;

/**
 * Objects that can be persisted as Nodes.
 * @author jmhill
 *
 */
public interface Nodeable extends Base {
	
	/**
	 * The name of this object
	 * @return
	 */
	public String getName();
	
	/**
	 * The name of this object
	 * @param name
	 */
	public void setName(String name);
	
	/**
	 * The URL that will get the annotations for this object.
	 * @param annoations
	 */
	public void setAnnotations(String annotations);
	
	/**
	 * The URL that will get the annotations for this object.
	 * @return
	 */
	public String getAnnotations();
	
	/**
	 * The URL for the Access Control List (ACL).
	 * @param acl
	 */
	public void setAccessControlList(String acl);
	
	/**
	 * The URL for the Access Control List (ACL).
	 * @return
	 */
	public String getAccessControlList();
	
	/**
	 * The Id of this node's parent
	 * @param parentId
	 */
	public void setParentId(String parentId);
	
	/**
	 * The Id of this node's parent
	 * @return
	 */
	public String getParentId();
	
}
