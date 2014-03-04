package org.sagebionetworks.repo.model.principal;

/**
 * A bootstrap group
 * 
 * @author John
 *
 */
public class BootstrapGroup extends BootstrapPrincipal {

	BootstrapAlias groupAlias;

	public BootstrapAlias getGroupAlias() {
		return groupAlias;
	}

	public void setGroupAlias(BootstrapAlias groupAlias) {
		this.groupAlias = groupAlias;
	}

	
}
