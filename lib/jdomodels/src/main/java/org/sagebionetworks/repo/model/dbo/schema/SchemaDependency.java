package org.sagebionetworks.repo.model.dbo.schema;

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
}
