package org.sagebionetworks.controller.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.schema.JsonSchema;

/**
 * Describes the RequestBody of a request.
 * @author lli
 *
 */
public class RequestBodyModel {
	private boolean required;
	private String description;
	private String id;
	
	public boolean isRequired() {
		return required;
	}
	
	public RequestBodyModel withRequired(boolean required) {
		this.required = required;
		return this;
	}
	
	public String getDescription() {
		return description;
	}
	
	public RequestBodyModel withDescription(String description) {
		this.description = description;
		return this;
	}
	
	public String getId() {
		return id;
	}
	
	public RequestBodyModel withId(String id) {
		this.id = id;
		return this;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(description, required, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RequestBodyModel other = (RequestBodyModel) obj;
		return Objects.equals(description, other.description) && required == other.required
				&& Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "RequestBodyModel [required=" + required + ", description=" + description + ", schema=" + id + "]";
	}
}
