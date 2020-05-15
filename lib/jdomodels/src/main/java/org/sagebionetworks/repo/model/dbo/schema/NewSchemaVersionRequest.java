package org.sagebionetworks.repo.model.dbo.schema;

import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.schema.JsonSchema;

public class NewSchemaVersionRequest {

	private String organizationId;
	private String schemaName;
	private Long createdBy;
	private String semanticVersion;
	private JsonSchema jsonSchema;
	private List<SchemaDependency> dependencies;
	
	/**
	 * @return the organizationId
	 */
	public String getOrganizationId() {
		return organizationId;
	}

	/**
	 * @param organizationId the organizationId to set
	 */
	public NewSchemaVersionRequest withOrganizationId(String organizationId) {
		this.organizationId = organizationId;
		return this;
	}

	/**
	 * @return the schemaName
	 */
	public String getSchemaName() {
		return schemaName;
	}

	/**
	 * @param schemaName the schemaName to set
	 */
	public NewSchemaVersionRequest withSchemaName(String schemaName) {
		this.schemaName = schemaName;
		return this;
	}

	/**
	 * @return the createdBy
	 */
	public Long getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy the createdBy to set
	 */
	public NewSchemaVersionRequest withCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	/**
	 * @return the semanticVersion
	 */
	public String getSemanticVersion() {
		return semanticVersion;
	}

	/**
	 * @param semanticVersion the semanticVersion to set
	 */
	public NewSchemaVersionRequest withSemanticVersion(String semanticVersion) {
		this.semanticVersion = semanticVersion;
		return this;
	}

	/**
	 * @return the jsonSchema
	 */
	public JsonSchema getJsonSchema() {
		return jsonSchema;
	}

	/**
	 * @param jsonSchema the jsonSchema to set
	 */
	public NewSchemaVersionRequest withJsonSchema(JsonSchema jsonSchema) {
		this.jsonSchema = jsonSchema;
		return this;
	}

	/**
	 * @return the dependencies
	 */
	public List<SchemaDependency> getDependencies() {
		return dependencies;
	}

	/**
	 * @param dependencies the dependencies to set
	 */
	public NewSchemaVersionRequest withDependencies(List<SchemaDependency> dependencies) {
		this.dependencies = dependencies;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, dependencies, jsonSchema, organizationId, schemaName, semanticVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NewSchemaVersionRequest)) {
			return false;
		}
		NewSchemaVersionRequest other = (NewSchemaVersionRequest) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(dependencies, other.dependencies)
				&& Objects.equals(jsonSchema, other.jsonSchema) && Objects.equals(organizationId, other.organizationId)
				&& Objects.equals(schemaName, other.schemaName)
				&& Objects.equals(semanticVersion, other.semanticVersion);
	}

	@Override
	public String toString() {
		return "NewSchemaVersionRequest [organizationId=" + organizationId + ", schemaName=" + schemaName
				+ ", createdBy=" + createdBy + ", semanticVersion=" + semanticVersion + ", jsonSchema=" + jsonSchema
				+ ", dependencies=" + dependencies + "]";
	}

}
