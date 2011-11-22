package org.sagebionetworks.repo.model.registry;

public interface MigrationDataLoader {

	/**
	 * Load Entity Migration data.
	 * @return
	 */
	public EntityMigrationData loadMigrationData();
}
