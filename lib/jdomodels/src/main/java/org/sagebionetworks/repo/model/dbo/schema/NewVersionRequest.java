package org.sagebionetworks.repo.model.dbo.schema;

import java.util.Date;

public class NewVersionRequest {

	private String schemaId;
	private String semanticVersion;
	private Long createdBy;
	private Date createdOn;
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
	 * @return the createdOn
	 */
	public Date getCreatedOn() {
		return createdOn;
	}

	/**
	 * @param createdOn the createdOn to set
	 */
	public NewVersionRequest withCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
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

}
