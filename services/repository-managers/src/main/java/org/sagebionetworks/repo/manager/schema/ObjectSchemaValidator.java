package org.sagebionetworks.repo.manager.schema;

public interface ObjectSchemaValidator {

	/**
	 * Validate the given object against its JSON schema.
	 * 
	 * @param objectId
	 */
	public void validateObject(String objectId);

}
