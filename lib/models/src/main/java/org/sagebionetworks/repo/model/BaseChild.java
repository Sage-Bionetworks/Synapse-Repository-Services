package org.sagebionetworks.repo.model;

public interface BaseChild extends Base{
	
	public void setParentId(String parentId);
	
	public String getParentId();
}
