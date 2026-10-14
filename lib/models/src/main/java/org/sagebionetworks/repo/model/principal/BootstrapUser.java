package org.sagebionetworks.repo.model.principal;

/**
 * A bootstrap user.
 * 
 * @author John
 *
 */
public class BootstrapUser implements BootstrapPrincipal {

	private BootstrapAlias email;
	private BootstrapAlias userName;
	private Long id;

	public BootstrapAlias getEmail() {
		return email;
	}

	public BootstrapUser setEmail(BootstrapAlias email) {
		this.email = email;
		return this;
	}

	public BootstrapAlias getUserName() {
		return userName;
	}

	public BootstrapUser setUserName(BootstrapAlias userName) {
		this.userName = userName;
		return this;
	}

	@Override
	public Long getId() {
		return id;
	}

	public BootstrapUser setId(Long id) {
		this.id = id;
		return this;
	}
}
