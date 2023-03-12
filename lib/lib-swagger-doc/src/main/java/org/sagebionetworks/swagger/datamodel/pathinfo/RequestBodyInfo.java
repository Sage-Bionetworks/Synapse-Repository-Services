package org.sagebionetworks.swagger.datamodel.pathinfo;

import java.util.Map;
import java.util.Objects;

/**
 * The request body applicable to an operation.
 * @author lli
 *
 */
public class RequestBodyInfo {
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
}
