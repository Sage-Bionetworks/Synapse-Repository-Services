package org.sagebionetworks.openapi.model;

import java.util.Objects;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Provides connectivity information to a target server 
 * @author lli
 *
 */
public class ServerInfo implements JSONEntity {
	private String url;
	private String description;
	
	public String getUrl() {
		return url;
	}
	
	public ServerInfo withUrl(String url) {
		this.url = url;
		return this;
	}
	
	public String getDescription() {
		return description;
	}
	
	public ServerInfo withDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, url);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerInfo other = (ServerInfo) obj;
		return Objects.equals(description, other.description) && Objects.equals(url, other.url);
	}

	@Override
	public String toString() {
		return "ServerInfo [url=" + url + ", description=" + description + "]";
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (url == null) {
			throw new IllegalArgumentException("The 'url' field is required.");
		}
		writeTo.put("url", url);
		if (description != null) {
			writeTo.put("description", description);
		}
		return writeTo;
	}
}
