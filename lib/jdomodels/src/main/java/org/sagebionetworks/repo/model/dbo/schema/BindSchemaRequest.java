package org.sagebionetworks.repo.model.dbo.schema;

import java.util.Objects;

import org.sagebionetworks.repo.model.schema.BoundObjectType;

/**
 * Request to bind an objectId, objectType pair to a JsonSchema.
 *
 */
public class BindSchemaRequest {

	private String schemaId;
	private String versionId;
	private Long objectId;
	private BoundObjectType objectType;
	private Long createdBy;

	/**
	 * @return the schemaId
	 */
	public String getSchemaId() {
		return schemaId;
	}

	/**
	 * @param schemaId the schemaId to set
	 */
	public BindSchemaRequest withSchemaId(String schemaId) {
		this.schemaId = schemaId;
		return this;
	}

	/**
	 * @return the versionId
	 */
	public String getVersionId() {
		return versionId;
	}

	/**
	 * @param versionId the versionId to set
	 */
	public BindSchemaRequest withVersionId(String versionId) {
		this.versionId = versionId;
		return this;
	}

	/**
	 * @return the objectId
	 */
	public Long getObjectId() {
		return objectId;
	}

	/**
	 * @param objectId the objectId to set
	 */
	public BindSchemaRequest withObjectId(Long objectId) {
		this.objectId = objectId;
		return this;
	}

	/**
	 * @return the objectType
	 */
	public BoundObjectType getObjectType() {
		return objectType;
	}

	/**
	 * @param objectType the objectType to set
	 */
	public BindSchemaRequest withObjectType(BoundObjectType objectType) {
		this.objectType = objectType;
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
	public BindSchemaRequest withCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, objectId, objectType, schemaId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BindSchemaRequest)) {
			return false;
		}
		BindSchemaRequest other = (BindSchemaRequest) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(objectId, other.objectId)
				&& objectType == other.objectType && Objects.equals(schemaId, other.schemaId);
	}

	@Override
	public String toString() {
		return "BindSchemaRequest [schemaId=" + schemaId + ", objectId=" + objectId + ", objectType=" + objectType
				+ ", createdBy=" + createdBy + "]";
	}

}
