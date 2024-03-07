package org.sagebionetworks.openapi.model.pathinfo;

import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Describes the response information for a particular status code.
 * @author lli
 *
 */
public class ResponseInfo implements JSONEntity {
	private String description;
	// maps contentType -> Schema
	private Map<String, Schema> content;
	
	public String getDescription() {
		return description;
	}
	
	public ResponseInfo withDescription(String description) {
		this.description = description;
		return this;
	}
	
	public Map<String, Schema> getContent() {
		return content;
	}
	
	public ResponseInfo withContent(Map<String, Schema> content) {
		this.content = content;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(content, description);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResponseInfo other = (ResponseInfo) obj;
		return Objects.equals(content, other.content) && Objects.equals(description, other.description);
	}

	@Override
	public String toString() {
		return "ResponseInfo [description=" + description + ", content=" + content + "]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (description == null) {
			throw new IllegalArgumentException("The 'description' field is required.");
		}
		writeTo.put("description", description);
		
		if (this.content != null && !this.content.isEmpty()) {
			JSONObjectAdapter content = writeTo.createNew();
			populateContent(content);
			writeTo.put("content", content);
		}
		return writeTo;
	}
	
	void populateContent(JSONObjectAdapter content) throws JSONObjectAdapterException {
		for (String contentType : this.content.keySet()) {
			Schema schema = this.content.get(contentType);
			content.put(contentType, schema.writeToJSONObject(content.createNew()));
		}
	}
}
