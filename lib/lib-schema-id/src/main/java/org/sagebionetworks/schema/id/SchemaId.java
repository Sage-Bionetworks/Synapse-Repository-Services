package org.sagebionetworks.schema.id;

import java.util.Objects;

import org.sagebionetworks.schema.element.Element;
import org.sagebionetworks.schema.semantic.version.SemanticVersion;

public final class SchemaId extends Element {

	public static final String DASH = "-";
	private final OrganizationName organizationName;
	private final SchemaName schemaName;
	private final SemanticVersion semanticVersion;

	public SchemaId(OrganizationName organizationName, SchemaName schemaName, SemanticVersion semanticVersion) {
		super();
		if (organizationName == null) {
			throw new IllegalArgumentException("OrganizationName cannot be null");

		}
		if (schemaName == null) {
			throw new IllegalArgumentException("SchemaName cannot be null");
		}
		this.organizationName = organizationName;
		this.schemaName = schemaName;
		this.semanticVersion = semanticVersion;
	}

	
	/**
	 * @return the organizationName
	 */
	public OrganizationName getOrganizationName() {
		return organizationName;
	}


	/**
	 * @return the schemaName
	 */
	public SchemaName getSchemaName() {
		return schemaName;
	}


	/**
	 * @return the semanticVersion
	 */
	public SemanticVersion getSemanticVersion() {
		return semanticVersion;
	}


	@Override
	public void toString(StringBuilder builder) {
		organizationName.toString(builder);
		builder.append(DASH);
		schemaName.toString(builder);
		if (semanticVersion != null) {
			builder.append(DASH);
			semanticVersion.toString(builder);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(organizationName, schemaName, semanticVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SchemaId)) {
			return false;
		}
		SchemaId other = (SchemaId) obj;
		return Objects.equals(organizationName, other.organizationName) && Objects.equals(schemaName, other.schemaName)
				&& Objects.equals(semanticVersion, other.semanticVersion);
	}

}
