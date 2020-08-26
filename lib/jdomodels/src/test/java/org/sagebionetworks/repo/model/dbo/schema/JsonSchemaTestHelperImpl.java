package org.sagebionetworks.repo.model.dbo.schema;

import java.util.ArrayList;

import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JsonSchemaTestHelperImpl implements JsonSchemaTestHelper {

	private JsonSchemaDao jsonSchemaDao;
	private OrganizationDao organizationDao;

	@Autowired
	public JsonSchemaTestHelperImpl(JsonSchemaDao jsonSchemaDao, OrganizationDao organizationDao) {
		super();
		this.jsonSchemaDao = jsonSchemaDao;
		this.organizationDao = organizationDao;
	}

	/**
	 * Helper to create a new JSON schema with the given $id
	 * 
	 * @param id
	 * @param index
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public JsonSchemaVersionInfo createNewSchemaVersion(Long createdBy, String id, int index)
			throws JSONObjectAdapterException {
		ArrayList<SchemaDependency> dependencies = null;
		return createNewSchemaVersion(createdBy, id, index, dependencies);
	}

	/**
	 * Helper to create a new JSON schema with the given $id
	 * 
	 * @param schema
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	@Override
	public JsonSchemaVersionInfo createNewSchemaVersion(Long createdBy, String id, int index,
			ArrayList<SchemaDependency> dependencies) throws JSONObjectAdapterException {
		SchemaId schemaId = SchemaIdParser.parseSchemaId(id);
		String organizationName = schemaId.getOrganizationName().toString();
		String schemaName = schemaId.getSchemaName().toString();
		String semanticVersion = null;
		if (schemaId.getSemanticVersion() != null) {
			semanticVersion = schemaId.getSemanticVersion().toString();
		}
		Organization organization = createOrganization(createdBy, organizationName);
		try {
			// sleep to ensure the organization created on is earlier than the schema's created on.
			Thread.sleep(10L);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		JsonSchema schema = new JsonSchema();
		schema.set$id(id);
		schema.setDescription("index:" + index);
		return jsonSchemaDao.createNewSchemaVersion(new NewSchemaVersionRequest()
				.withOrganizationId(organization.getId()).withCreatedBy(createdBy).withJsonSchema(schema)
				.withSchemaName(schemaName).withSemanticVersion(semanticVersion).withDependencies(dependencies));
	}

	/**
	 * Create or get an organization with the given name
	 * 
	 * @param organizationName
	 * @return
	 */
	@Override
	public Organization createOrganization(Long createdBy, String organizationName) {
		try {
			return organizationDao.getOrganizationByName(organizationName);
		} catch (NotFoundException e) {
			return organizationDao.createOrganization(organizationName, createdBy);
		}
	}

	@Override
	public void truncateAll() {
		jsonSchemaDao.truncateAll();
		organizationDao.truncateAll();
	}

	@Override
	public JsonSchemaObjectBinding bindSchemaToObject(Long createdBy, String schema$id, Long objectId,
			BoundObjectType objectType) {
		SchemaId parsedId = SchemaIdParser.parseSchemaId(schema$id);
		String schemaId = jsonSchemaDao.getSchemaId(parsedId.getOrganizationName().toString(),
				parsedId.getSchemaName().toString());
		String versionId = null;
		if (parsedId.getSemanticVersion() != null) {
			versionId = jsonSchemaDao.getVersionId(parsedId.getOrganizationName().toString(),
					parsedId.getSchemaName().toString(), parsedId.getSemanticVersion().toString());
		}
		return jsonSchemaDao.bindSchemaToObject(new BindSchemaRequest().withCreatedBy(createdBy).withObjectId(objectId)
				.withObjectType(objectType).withSchemaId(schemaId).withVersionId(versionId));
	}

}
