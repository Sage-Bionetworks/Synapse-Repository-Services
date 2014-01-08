package org.sagebionetworks.repo.model.principal;

/**
 * A bootstrap group
 * 
 * @author John
 *
 */
public class BootstrapGroup extends BootstrapPrincipal {

	String groupName;

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
}
