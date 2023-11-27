package org.sagebionetworks.openapi.datamodel.pathinfo;

import java.util.Objects;

import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Describes a single operation parameter.
 * @author lli
 *
 */
public class ParameterInfo implements JSONEntity {
	private String name;
	private String in;
	private boolean required;
	private String description;
	private JsonSchema schema;
	
	public String getName() {
		return name;
	}
	
	public ParameterInfo withName(String name) {
		this.name = name;
		return this;
	}
	
	public String getIn() {
		return in;
	}
	
	public ParameterInfo withIn(String in) {
		this.in = in;
		return this;
	}
	
	public boolean isRequired() {
		return required;
	}
	
	public ParameterInfo withRequired(boolean required) {
		this.required = required;
		return this;
	}
	
	public String getDescription() {
		return description;
	}
	
	public ParameterInfo withDescription(String description) {
		this.description = description;
		return this;
	}
	
	public JsonSchema getSchema() {
		return schema;
	}
	
	public ParameterInfo withSchema(JsonSchema schema) {
		this.schema = schema;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, in, name, required, schema);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParameterInfo other = (ParameterInfo) obj;
		return Objects.equals(description, other.description) && Objects.equals(in, other.in)
				&& Objects.equals(name, other.name) && required == other.required
				&& Objects.equals(schema, other.schema);
	}

	@Override
	public String toString() {
		return "ParameterInfo [name=" + name + ", in=" + in + ", required=" + required + ", description=" + description
				+ ", schema=" + schema + "]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (name == null) {
			throw new IllegalArgumentException("The 'name' field must not be null.");
		}
		if (in == null) {
			throw new IllegalArgumentException("The 'in' field must not be null.");
		}
		writeTo.put("name", name);
		writeTo.put("in", in);
		writeTo.put("required", required);

		if (description != null) {
			writeTo.put("description", description);
		}
		if (schema != null) {
			writeTo.put("schema", schema.writeToJSONObject(writeTo.createNew()));
		}
		return writeTo;
	}
}
