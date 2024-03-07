package org.sagebionetworks.openapi.model.pathinfo;

import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * The request body applicable to an operation.
 * @author lli
 *
 */
public class RequestBodyInfo implements JSONEntity {
	// This maps content-type -> the appropriate schema
	private Map<String, Schema> content;
	private boolean required;
	
	public Map<String, Schema> getContent() {
		return content;
	}
	
	public RequestBodyInfo withContent(Map<String, Schema> content) {
		this.content = content;
		return this;
	}
	
	public boolean isRequired() {
		return required;
	}
	
	public RequestBodyInfo withRequired(boolean required) {
		this.required = required;
		return this;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(content, required);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RequestBodyInfo other = (RequestBodyInfo) obj;
		return Objects.equals(content, other.content) && required == other.required;
	}

	@Override
	public String toString() {
		return "RequestBodyInfo [content=" + content + ", required=" + required + "]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (content == null) {
			throw new IllegalArgumentException("The 'content' field must not be null.");
		}
		if (content.isEmpty()) {
			throw new IllegalArgumentException("The 'content' field must not be empty.");
		}
		
		JSONObjectAdapter content = writeTo.createNew();
		for (String contentType : this.content.keySet()) {
			Schema schema = this.content.get(contentType);
			content.put(contentType, schema.writeToJSONObject(writeTo.createNew()));
		}
		writeTo.put("content", content);
		writeTo.put("required", required);

		return writeTo;
	}
}
