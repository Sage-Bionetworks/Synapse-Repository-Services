package org.sagebionetworks.repo.model;

public interface AccessControlListDAO {

	public AccessControlList getACL(String resourceId);
	
	public AccessControlList createACL(AccessControlList acl);
	
	public AccessControlList updateACL(AccessControlList acl);
	
	public void deleteACL(String resourceId);
	
}
