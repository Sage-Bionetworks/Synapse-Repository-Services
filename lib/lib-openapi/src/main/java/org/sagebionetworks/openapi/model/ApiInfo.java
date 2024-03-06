package org.sagebionetworks.openapi.model;

import java.util.Objects;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Provides metadata about the API being described by the OpenAPI specification.
 * 
 * @author lli
 *
 */
public class ApiInfo implements JSONEntity {
	private String title;
	private String summary;
	private String version;
	
	public String getTitle() {
		return title;
	}
	
	public ApiInfo withTitle(String title) {
		this.title = title;
		return this;
	}
	
	public String getSummary() {
		return summary;
	}
	
	public ApiInfo withSummary(String summary) {
		this.summary = summary;
		return this;
	}
	
	public String getVersion() {
		return version;
	}
	
	public ApiInfo withVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(summary, title, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApiInfo other = (ApiInfo) obj;
		return Objects.equals(summary, other.summary) && Objects.equals(title, other.title)
				&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "ApiInfo [title=" + title + ", summary=" + summary + ", version=" + version + "]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (title == null) {
			throw new IllegalArgumentException("The 'title' is a required attribute of ApiInfo.");
		}
		writeTo.put("title", title);
		if (version != null) {
			writeTo.put("version", version);
		}
		if (summary != null) {
			writeTo.put("summary", summary);
		}
		return writeTo;
	}
}
