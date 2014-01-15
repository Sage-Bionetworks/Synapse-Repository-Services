package org.sagebionetworks.repo.model.principal;

/**
 * A bootstrap user.
 * @author John
 *
 */
public class BootstrapUser extends BootstrapPrincipal {
	
	BootstrapAlias email;
	BootstrapAlias userName;
	public BootstrapAlias getEmail() {
		return email;
	}
	public void setEmail(BootstrapAlias email) {
		this.email = email;
	}
	public BootstrapAlias getUserName() {
		return userName;
	}
	public void setUserName(BootstrapAlias userName) {
		this.userName = userName;
	}
	

}
