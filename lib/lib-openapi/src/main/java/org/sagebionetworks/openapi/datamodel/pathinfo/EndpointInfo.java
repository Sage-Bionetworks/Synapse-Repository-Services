package org.sagebionetworks.openapi.datamodel.pathinfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Metadata about a specific endpoint.
 * @author lli
 *
 */
public class EndpointInfo implements JSONEntity {
	private List<String> tags;
	private String operationId;
	private List<ParameterInfo> parameters;
	private RequestBodyInfo requestBody;
	private Map<String, ResponseInfo> responses;
	private Map<String, String[]> securityRequirements;
	
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

	public Map<String, String[]> getSecurityRequirements() {
		return securityRequirements;
	}

	public EndpointInfo withSecurityRequirements(Map<String, String[]> securityRequirements) {
		this.securityRequirements = securityRequirements;
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
				&& Objects.equals(tags, other.tags) && securityRequirementsAreEqual(securityRequirements, other.securityRequirements);
	}
	
	@Override
	public String toString() {
		return "EndpointInfo [tags=" + tags + ", operationId=" + operationId + ", parameters=" + parameters
				+ ", requestBody=" + requestBody + ", responses=" + responses + ", securityRequirements=" + securityRequirements + "]";
	}
	
	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (this.responses == null) {
			throw new IllegalArgumentException("Responses must not be null.");
		}
		if (this.responses.isEmpty()) {
			throw new IllegalArgumentException("Responses must not be empty.");
		}
		
		if (this.tags != null) {
			JSONArrayAdapter tags = writeTo.createNewArray();
			populateTags(tags);
			writeTo.put("tags", tags);
		}
		
		if (this.operationId != null) {
			writeTo.put("operationId", operationId);
		}
		
		if (this.parameters != null) {
			JSONArrayAdapter parameters = writeTo.createNewArray();
			populateParameters(parameters);
			writeTo.put("parameters", parameters);
		}
		
		if (this.requestBody != null) {
			writeTo.put("requestBody", requestBody.writeToJSONObject(writeTo.createNew()));
		}

		if (this.securityRequirements != null && !this.securityRequirements.isEmpty()) {
			JSONArrayAdapter securityArrayAdapter = writeTo.createNewArray();

			int i = 0;
			for (Map.Entry<String, String[]> requirement: this.securityRequirements.entrySet()) {
				JSONArrayAdapter requirementArrayAdapter = writeTo.createNewArray();
				JSONObjectAdapter adapter = writeTo.createNew();

				String[] scopes = requirement.getValue();
				for (int j = 0; j < requirement.getValue().length; j++) {
					requirementArrayAdapter.put(j, scopes[j]);
				}

				adapter.put(requirement.getKey(), requirementArrayAdapter);
				securityArrayAdapter.put(i, adapter);
				i++;
			}

			writeTo.put("security",securityArrayAdapter);
		}
		
		JSONObjectAdapter responses = writeTo.createNew();
		populateResponses(responses);
		writeTo.put("responses", responses);

		return writeTo;
	}
	
	void populateResponses(JSONObjectAdapter responses) throws JSONObjectAdapterException {
		for (String responseCode : this.responses.keySet()) {
			ResponseInfo respose = this.responses.get(responseCode);
			responses.put(responseCode, respose.writeToJSONObject(responses.createNew()));
		}
	}
	
	void populateParameters(JSONArrayAdapter parameters) throws JSONObjectAdapterException {
		for (int i = 0; i < this.parameters.size(); i++) {
			parameters.put(i, this.parameters.get(i).writeToJSONObject(parameters.createNew()));
		}
	}
	
	void populateTags(JSONArrayAdapter tags) throws JSONObjectAdapterException {
		for (int i = 0; i < this.tags.size(); i++) {
			tags.put(i, this.tags.get(i));
		}
	}

	boolean securityRequirementsAreEqual(Map<String, String[]> map1, Map<String, String[]> map2) {
		// Check if both maps are null or have different sizes
		if (map1 == null && map2 == null) {
			return true;
		} else if (map1 == null || map2 == null || map1.size() != map2.size()) {
			return false;
		}

		// Iterate through the entries of the first map
		for (Map.Entry<String, String[]> entry : map1.entrySet()) {
			String key = entry.getKey();
			String[] value1 = entry.getValue();
			String[] value2 = map2.get(key);

			// Check if the key is present in the second map and the values are equal
			if (value2 == null || !Arrays.equals(value1, value2)) {
				return false;
			}
		}

		// Maps are equal
		return true;
	}
}
