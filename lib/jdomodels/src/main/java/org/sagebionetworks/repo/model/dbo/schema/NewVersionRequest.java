package org.sagebionetworks.repo.model.dbo.schema;

import java.util.Objects;

public class NewVersionRequest {

	private String schemaId;
	private String semanticVersion;
	private Long createdBy;
	private String blobId;

	/**
	 * @return the schemaId
	 */
	public String getSchemaId() {
		return schemaId;
	}

	/**
	 * @param schemaId the schemaId to set
	 */
	public NewVersionRequest withSchemaId(String schemaId) {
		this.schemaId = schemaId;
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
	public NewVersionRequest withSemanticVersion(String semanticVersion) {
		this.semanticVersion = semanticVersion;
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
	public NewVersionRequest withCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	/**
	 * @return the blobId
	 */
	public String getBlobId() {
		return blobId;
	}

	/**
	 * @param blobId the blobId to set
	 */
	public NewVersionRequest withBlobId(String blobId) {
		this.blobId = blobId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(blobId, createdBy, schemaId, semanticVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NewVersionRequest)) {
			return false;
		}
		NewVersionRequest other = (NewVersionRequest) obj;
		return Objects.equals(blobId, other.blobId) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(schemaId, other.schemaId) && Objects.equals(semanticVersion, other.semanticVersion);
	}

}
