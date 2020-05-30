package org.sagebionetworks.repo.model.dbo.schema;

import java.util.ArrayList;

import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface JsonSchemaTestHelper {

	/**
	 * Create a new Organization.
	 * 
	 * @param createdBy
	 * @param organizationName
	 * @return
	 */
	Organization createOrganization(Long createdBy, String organizationName);

	/**
	 * Create a new JSON Schema
	 * 
	 * @param createdBy    The ID of the user creating the schema.
	 * @param $id          The $id of the new JSON schema.
	 * @param index        bump the index when creating schemas in a loop.
	 * @param dependencies The dependencies of the schema. Optional.
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	JsonSchemaVersionInfo createNewSchemaVersion(Long createdBy, String $id, int index,
			ArrayList<SchemaDependency> dependencies) throws JSONObjectAdapterException;

	/**
	 * Create a new JSON Schema
	 * 
	 * @param createdBy    The ID of the user creating the schema.
	 * @param $id          The $id of the new JSON schema.
	 * @param index        bump the index when creating schemas in a loop.
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	JsonSchemaVersionInfo createNewSchemaVersion(Long createdBy, String $id, int index)
			throws JSONObjectAdapterException;

	/**
	 * Truncate all JSON schema an Organization data.
	 */
	void truncateAll();

	/**
	 * Bind the given schema $id to the provide objectId and type.
	 * @param createdBy
	 * @param schema$id
	 * @param objectId
	 * @param objectType
	 * @return
	 */
	JsonSchemaObjectBinding bindSchemaToObject(Long createdBy, String schema$id, Long objectId, BoundObjectType objectType);

}
