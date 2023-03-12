package org.sagebionetworks.swagger.datamodel.pathinfo;

import java.util.Map;
import java.util.Objects;

/**
 * Describes the response information for a particular status code.
 * @author lli
 *
 */
public class ResponseInfo {
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
}
