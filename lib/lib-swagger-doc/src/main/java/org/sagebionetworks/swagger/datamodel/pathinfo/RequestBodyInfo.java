package org.sagebionetworks.swagger.datamodel.pathinfo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

public class RequestBodyInfo {
	private String description;
	private Map<String, JSONObject> contentTypeToSchema;
	private boolean isRequired;
	
	public RequestBodyInfo() {
		this("");
	}
	
	public RequestBodyInfo(String description) {
		this(description, true);
	}
	
	public RequestBodyInfo(String description, boolean isRequired) {
		this.description = description;
		this.isRequired = isRequired;
		this.contentTypeToSchema = new LinkedHashMap<>();
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void setIsRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}
	
	public boolean isRequired() {
		return this.isRequired;
	}
	
	public void addContentTypeAndSchema(String contentType, JSONObject schema) {
		this.contentTypeToSchema.put(contentType, schema);
	}
	
	public Map<String, JSONObject> getContentTypeToSchema() {
		return new LinkedHashMap<>(this.contentTypeToSchema);
	}
}
