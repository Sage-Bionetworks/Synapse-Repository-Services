package org.sagebionetworks.repo.model.principal;

/**
 * Bootstrap for an alias
 * @author John
 *
 */
public class BootstrapAlias {
	
	private Long aliasId;
	private String aliasName;
	public Long getAliasId() {
		return aliasId;
	}
	public BootstrapAlias setAliasId(Long aliasId) {
		this.aliasId = aliasId;
		return this;
	}
	public String getAliasName() {
		return aliasName;
	}
	public BootstrapAlias setAliasName(String aliasName) {
		this.aliasName = aliasName;
		return this;
	}

}
