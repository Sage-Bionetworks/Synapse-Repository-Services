package org.sagebionetworks.swagger.datamodel.pathinfo;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata about a specific endpoint.
 * @author lli
 *
 */
public class EndpointInfo {
	private List<String> tags;
	private String operationId;
	private List<ParameterInfo> parameters;
	private RequestBodyInfo requestBody;
	private Map<String, ResponseInfo> responses;
	
	public List<String> getTags() {
		return tags;
	}
	public EndpointInfo withTags(List<String> tags) {
		this.tags = tags;
		return this;
	}
	
	public String getOperationId() {
		return operationId;
	}
	
	public EndpointInfo withOperationId(String operationId) {
		this.operationId = operationId;
		return this;
	}
	
	public List<ParameterInfo> getParameters() {
		return parameters;
	}
	
	public EndpointInfo withParameters(List<ParameterInfo> parameters) {
		this.parameters = parameters;
		return this;
	}
	
	public RequestBodyInfo getRequestBody() {
		return requestBody;
	}
	
	public EndpointInfo withRequestBody(RequestBodyInfo requestBody) {
		this.requestBody = requestBody;
		return this;
	}
	
	public Map<String, ResponseInfo> getResponses() {
		return responses;
	}
	
	public EndpointInfo withResponses(Map<String, ResponseInfo> responses) {
		this.responses = responses;
		return this;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(operationId, parameters, requestBody, responses, tags);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EndpointInfo other = (EndpointInfo) obj;
		return Objects.equals(operationId, other.operationId) && Objects.equals(parameters, other.parameters)
				&& Objects.equals(requestBody, other.requestBody) && Objects.equals(responses, other.responses)
				&& Objects.equals(tags, other.tags);
	}
	
	@Override
	public String toString() {
		return "EndpointInfo [tags=" + tags + ", operationId=" + operationId + ", parameters=" + parameters
				+ ", requestBody=" + requestBody + ", responses=" + responses + "]";
	}
}
