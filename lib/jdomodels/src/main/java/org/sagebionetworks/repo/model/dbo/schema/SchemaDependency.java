package org.sagebionetworks.repo.model.dbo.schema;

import java.util.Objects;

public class SchemaDependency {
	
	String dependsOnSchemaId;
	String dependsOnVersionId;
	/**
	 * @return the dependsOnSchemaId
	 */
	public String getDependsOnSchemaId() {
		return dependsOnSchemaId;
	}
	/**
	 * @param dependsOnSchemaId the dependsOnSchemaId to set
	 */
	public SchemaDependency withDependsOnSchemaId(String dependsOnSchemaId) {
		this.dependsOnSchemaId = dependsOnSchemaId;
		return this;
	}
	/**
	 * @return the dependsOnVersionId
	 */
	public String getDependsOnVersionId() {
		return dependsOnVersionId;
	}
	/**
	 * @param dependsOnVersionId the dependsOnVersionId to set
	 */
	public SchemaDependency withDependsOnVersionId(String dependsOnVersionId) {
		this.dependsOnVersionId = dependsOnVersionId;
		return this;
	}
	@Override
	public int hashCode() {
		return Objects.hash(dependsOnSchemaId, dependsOnVersionId);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SchemaDependency)) {
			return false;
		}
		SchemaDependency other = (SchemaDependency) obj;
		return Objects.equals(dependsOnSchemaId, other.dependsOnSchemaId)
				&& Objects.equals(dependsOnVersionId, other.dependsOnVersionId);
	}
	@Override
	public String toString() {
		return "SchemaDependency [dependsOnSchemaId=" + dependsOnSchemaId + ", dependsOnVersionId=" + dependsOnVersionId
				+ "]";
	}
	
	
}
