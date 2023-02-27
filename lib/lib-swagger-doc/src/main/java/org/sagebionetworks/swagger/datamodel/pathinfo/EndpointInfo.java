package org.sagebionetworks.swagger.datamodel.pathinfo;

import java.util.ArrayList;
import java.util.List;

public class EndpointInfo {
	private List<String> tags;
	private String operationID;
	private List<ParameterInfo> parameters;
	private RequestBodyInfo requestBody;
	private ResponsesInfo responses;
	
	public EndpointInfo(List<String> tags, String operationID, List<ParameterInfo> parameters,
			RequestBodyInfo requestBody, ResponsesInfo responses) {
		this.tags = tags;
		this.operationID = operationID;
		this.parameters = parameters;
		this.requestBody = requestBody;
		this.responses = responses;
	}
	
	public List<String> getTags() {
		return new ArrayList<>(this.tags);
	}
	
	public String getOperationID() {
		return this.operationID;
	}
	
	public List<ParameterInfo> getParameters() {
		return new ArrayList<>(this.parameters);
	}
	
	public ResponsesInfo getResponses() {
		return this.responses;
	}
	
	public RequestBodyInfo getRequestBody() {
		return this.requestBody;
	}
	
}
