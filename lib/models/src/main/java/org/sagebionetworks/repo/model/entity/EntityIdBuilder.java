package org.sagebionetworks.repo.model.entity;

/**
 * Simple builder for an entity ID
 *
 */
public class EntityIdBuilder {
	
	Long id;
	Long version;
	
	public EntityIdBuilder setId(Long id) {
		this.id = id;
		return this;
	}
	public EntityIdBuilder setVersion(Long version) {
		this.version = version;
		return this;
	}

	EntityId build() {
		return new EntityId(id, version);
	}
}
