package org.sagebionetworks.swagger.datamodel.pathinfo;

import java.util.ArrayList;
import java.util.List;

public class EndpointInfo {
	private List<String> tags;
	private String operationID;
	private List<ParameterInfo> parameters;
	private ResponsesInfo responses;
	private RequestBodyInfo requestBody;
	
	public EndpointInfo(String operationID, List<String> tags, List<ParameterInfo> parameters, ResponsesInfo responses,
			RequestBodyInfo requestBody) {
		this.operationID = operationID;
		this.tags = tags;
		this.parameters = parameters;
		this.responses = responses;
		this.requestBody = requestBody;
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
