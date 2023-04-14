package org.sagebionetworks.controller.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.schema.JsonSchema;

/**
 * Describes a parameter passed into a method in a controller.
 * @author lli
 *
 */
public class ParameterModel {
	private String name;
	private ParameterLocation in;
	private boolean required;
	private String description;
	private JsonSchema schema;
	
	public String getName() {
		return name;
	}

	public ParameterModel withName(String name) {
		this.name = name;
		return this;
	}
	
	public String getDescription() {
		return description;
	}

	public ParameterModel withDescription(String description) {
		this.description = description;
		return this;
	}
	
	public ParameterLocation getIn() {
		return in;
	}
	
	public ParameterModel withIn(ParameterLocation in) {
		this.in = in;
		return this;
	}
	
	public boolean isRequired() {
		return required;
	}
	
	public ParameterModel withRequired(boolean required) {
		if (this.in == null) {
			throw new IllegalArgumentException("The 'in' field must be set before 'required' field.");
		}
		if (this.in == ParameterLocation.path && !required) {
			throw new IllegalArgumentException("Parameters must be required for path variables.");
		}
		this.required = required;
		return this;
	}
	
	public JsonSchema getSchema() {
		return schema;
	}
	
	public ParameterModel withSchema(JsonSchema schema) {
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
		ParameterModel other = (ParameterModel) obj;
		return Objects.equals(description, other.description) && in == other.in && Objects.equals(name, other.name)
				&& required == other.required && Objects.equals(schema, other.schema);
	}

	@Override
	public String toString() {
		return "ParameterModel [name=" + name + ", in=" + in + ", required=" + required + ", schema=" + schema
				+ ", description=" + description + "]";
	}
}
