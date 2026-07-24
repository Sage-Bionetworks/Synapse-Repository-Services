package org.sagebionetworks.repo.model.principal;

/**
 * A bootstrap group
 * 
 * @author John
 *
 */
public class BootstrapGroup implements BootstrapPrincipal {

	private BootstrapAlias groupAlias;
	private Long id;

	public BootstrapAlias getGroupAlias() {
		return groupAlias;
	}

	public BootstrapGroup setGroupAlias(BootstrapAlias groupAlias) {
		this.groupAlias = groupAlias;
		return this;
	}

	@Override
	public Long getId() {
		return id;
	}

	public BootstrapGroup setId(Long id) {
		this.id = id;
		return this;
	}

}
