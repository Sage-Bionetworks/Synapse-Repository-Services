package org.sagebionetworks.repo.model;

/**
 * Any entity that can have parent should implement this interface.
 * 
 * @author jmhill
 *
 */
public interface BaseChild extends Nodeable {
	
	public void setParentId(String parentId);
	
	public String getParentId();
	
}
