package org.sagebionetworks.repo.model.dbo.schema;

import java.util.Objects;

public class NewSchemaRequest {

	private String organizationId;
	private String schemaName;
	private Long createdBy;
	
	/**
	 * @return the organizationId
	 */
	public String getOrganizationId() {
		return organizationId;
	}
	/**
	 * @return the schemaName
	 */
	public String getSchemaName() {
		return schemaName;
	}
	/**
	 * @return the createdBy
	 */
	public Long getCreatedBy() {
		return createdBy;
	}
	/**
	 * @param organizationId the organizationId to set
	 */
	public NewSchemaRequest withOrganizationId(String organizationId) {
		this.organizationId = organizationId;
		return this;
	}
	/**
	 * @param schemaName the schemaName to set
	 */
	public NewSchemaRequest withSchemaName(String schemaName) {
		this.schemaName = schemaName;
		return this;
	}
	/**
	 * @param createdBy the createdBy to set
	 */
	public NewSchemaRequest withCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}
	@Override
	public int hashCode() {
		return Objects.hash(createdBy, organizationId, schemaName);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NewSchemaRequest)) {
			return false;
		}
		NewSchemaRequest other = (NewSchemaRequest) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(organizationId, other.organizationId)
				&& Objects.equals(schemaName, other.schemaName);
	}
	
	
}
