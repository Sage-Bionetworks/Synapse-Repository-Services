package org.sagebionetworks.repo.model.entity;

/**
 * Simple builder for an entity ID
 *
 */
public class IdAndVersionBuilder {
	
	Long id;
	Long version;
	
	public IdAndVersionBuilder setId(Long id) {
		this.id = id;
		return this;
	}
	public IdAndVersionBuilder setVersion(Long version) {
		this.version = version;
		return this;
	}

	public IdAndVersion build() {
		return new IdAndVersion(id, version);
	}
}
