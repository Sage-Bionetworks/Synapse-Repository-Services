package org.sagebionetworks.swagger.datamodel;

import java.util.Objects;

/**
 * Provides connectivity information to a target server 
 * @author lli
 *
 */
public class ServerInfo {
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
	
	
}
