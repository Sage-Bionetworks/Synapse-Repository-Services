package org.sagebionetworks.repo.model.dbo.principal;

/**
 * A bootstrap user.
 * @author John
 *
 */
public class BootstrapUser extends BootstrapPrincipal {
	
	String email;
	String userName;
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}

}
