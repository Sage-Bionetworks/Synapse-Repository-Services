package org.sagebionetworks.openapi.datamodel.pathinfo;

import java.util.Objects;

import org.sagebionetworks.repo.model.schema.JsonSchema;

/**
 * Represents the schema of a content type.
 * @author lli
 *
 */
public class Schema {
	private JsonSchema schema;

	public JsonSchema getSchema() {
		return schema;
	}

	public Schema withSchema(JsonSchema schema) {
		this.schema = schema;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(schema);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Schema other = (Schema) obj;
		return Objects.equals(schema, other.schema);
	}

	@Override
	public String toString() {
		return "Schema [schema=" + schema + "]";
	}
}
