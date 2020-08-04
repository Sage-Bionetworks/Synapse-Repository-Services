package org.sagebionetworks.repo.manager.schema;

public interface EntitySchemaManager {

	/**
	 * Validate the given Entity against its bound schema.
	 * 
	 * @param entityId
	 */
	public void validateEntityAgainstBoundSchema(String entityId);
}
