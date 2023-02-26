package org.sagebionetworks.swagger.datamodel;

public class ApiInfo {
	private String title;
	private String summary;
	private String version;
	
	public ApiInfo(String title, String version, String summary) {
		this.title = title;
		this.summary = summary;
		this.version = version;
	}
	
	public ApiInfo(String title, String version) {
		this(title, version, "");
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public String getSummary() {
		return this.summary;
	}
	
	public String getVersion() {
		return this.version;
	}
}
