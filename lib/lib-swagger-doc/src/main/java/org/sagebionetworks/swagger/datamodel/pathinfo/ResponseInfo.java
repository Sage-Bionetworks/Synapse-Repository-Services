package org.sagebionetworks.swagger.datamodel.pathinfo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

public class ResponseInfo {
	private String description;
	private Map<String, JSONObject> contentTypeToResponseSchema;
	
	public ResponseInfo() {
		this("");
	}
	
	public ResponseInfo(String description) {
		this.description = description;
		this.contentTypeToResponseSchema = new LinkedHashMap<>();
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void addContentTypeAndResponse(String contentType, JSONObject responseSchema) {
		this.contentTypeToResponseSchema.put(contentType, responseSchema);
	}
	
	public Map<String, JSONObject> getContentTypeToResponse() {
		return new LinkedHashMap<>(this.contentTypeToResponseSchema);
	}
}
