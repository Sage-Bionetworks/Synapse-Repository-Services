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
	private String id;
	
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
	
	public String getId() {
		return id;
	}
	
	public ParameterModel withId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, id, in, name, required);
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
		return Objects.equals(description, other.description) && Objects.equals(id, other.id) && in == other.in
				&& Objects.equals(name, other.name) && required == other.required;
	}

	@Override
	public String toString() {
		return "ParameterModel [name=" + name + ", in=" + in + ", required=" + required + ", description=" + description
				+ ", id=" + id + "]";
	}
}
