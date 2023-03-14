package org.sagebionetworks.openapi.datamodel;

import java.util.Objects;

/**
 * Provides metadata about the API being described by the OpenAPI specification.
 * 
 * @author lli
 *
 */
public class ApiInfo {
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
}
