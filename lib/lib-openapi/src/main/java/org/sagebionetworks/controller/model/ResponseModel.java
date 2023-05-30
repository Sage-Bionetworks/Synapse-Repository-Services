package org.sagebionetworks.controller.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.schema.JsonSchema;

/**
 * Represents the response of a method in a controller.
 * @author lli
 *
 */
public class ResponseModel {
	private int statusCode;
	private String description;
	private String contentType = "application/json";
	private String id;
	
	public int getStatusCode() {
		return statusCode;
	}
	
	public ResponseModel withStatusCode(int statusCode) {
		this.statusCode = statusCode;
		return this;
	}
	
	public String getDescription() {
		return description;
	}
	
	public ResponseModel withDescription(String description) {
		this.description = description;
		return this;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public ResponseModel withContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}
	
	public String getId() {
		return this.id;
	}
	
	public ResponseModel withId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentType, description, id, statusCode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResponseModel other = (ResponseModel) obj;
		return Objects.equals(contentType, other.contentType) && Objects.equals(description, other.description)
				&& Objects.equals(id, other.id) && statusCode == other.statusCode;
	}

	@Override
	public String toString() {
		return "ResponseModel [statusCode=" + statusCode + ", description=" + description + ", contentType="
				+ contentType + ", id=" + id + "]";
	}
}
