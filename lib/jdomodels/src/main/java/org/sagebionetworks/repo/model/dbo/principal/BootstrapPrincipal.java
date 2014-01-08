package org.sagebionetworks.repo.model.dbo.principal;

/**
 * Abstract class for bootstrap principals
 * @author John
 *
 */
public abstract class BootstrapPrincipal {

	Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}	
}
