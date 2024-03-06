package org.sagebionetworks.openapi.model.pathinfo;

import java.util.Objects;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Represents the schema of a content type.
 * @author lli
 *
 */
public class Schema implements JSONEntity {
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

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (schema == null) {
			throw new IllegalArgumentException("The 'schema' field must not be null.");
		}
		writeTo.put("schema", this.schema.writeToJSONObject(writeTo.createNew()));
		return writeTo;
	}
}
