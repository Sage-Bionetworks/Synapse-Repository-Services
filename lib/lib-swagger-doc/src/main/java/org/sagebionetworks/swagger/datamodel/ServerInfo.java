package org.sagebionetworks.swagger.datamodel;

public class ServerInfo {
	private String url;
	private String description;
	
	public ServerInfo(String url, String description) {
		this.url = url;
		this.description = description;
	}
	
	public String getUrl() {
		return this.url;
	}
	
	public String getDescription() {
		return this.description;
	}
}
