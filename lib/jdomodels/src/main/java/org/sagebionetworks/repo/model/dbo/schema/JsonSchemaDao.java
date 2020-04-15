package org.sagebionetworks.repo.model.dbo.schema;

import org.sagebionetworks.repo.model.schema.SchemaInfo;

public interface JsonSchemaDao {

	/**
	 * Create a JSON schema if it does not already exist.
	 * 
	 * @param schemaRoot
	 * @return The returned SchemaInfo.id will either be the ID of the newly created
	 *         schema or the ID of the existing schema.
	 */
	SchemaInfo createSchemaIfDoesNotExist(SchemaInfo schemaRoot);
	
	/**
	 * Lookup a SchemaInfo from an organization name and schema name.
	 * @param organizationName
	 * @param schemaName
	 * @return
	 */
	SchemaInfo getSchemaInfo(String organizationName, String schemaName);

	/**
	 * Truncate all data.
	 */
	void trunacteAll();

}
