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
	private String contentType;
	private JsonSchema schema;
	
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
	
	public JsonSchema getSchema() {
		return schema;
	}
	
	public ResponseModel withSchema(JsonSchema schema) {
		this.schema = schema;
		return this;
	}

	@Override
	public String toString() {
		return "ResponseModel [statusCode=" + statusCode + ", description=" + description + ", contentType="
				+ contentType + ", schema=" + schema + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentType, description, schema, statusCode);
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
				&& Objects.equals(schema, other.schema) && statusCode == other.statusCode;
	}
}
