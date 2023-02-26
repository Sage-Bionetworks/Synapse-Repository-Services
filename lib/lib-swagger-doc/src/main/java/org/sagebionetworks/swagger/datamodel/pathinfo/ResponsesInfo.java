package org.sagebionetworks.swagger.datamodel.pathinfo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;

public class ResponsesInfo {
	private Map<String, ResponseInfo> httpStatusCodeToResponse;
	
	public ResponsesInfo() {
		this.httpStatusCodeToResponse = new LinkedHashMap<>();
	}
	
	public void addStatusCodeWithResponse(String statusCode, ResponseInfo response) {
		this.httpStatusCodeToResponse.put(statusCode, response);
	}
	
	public Map<String, ResponseInfo> getStatusCodesAndResponses() {
		return new LinkedHashMap<>(this.httpStatusCodeToResponse);
	}
}