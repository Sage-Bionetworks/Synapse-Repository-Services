package org.sagebionetworks.controller.model;

import java.util.List;
import java.util.Objects;

/**
 * Metadata about a method in a controller.
 * @author lli
 *
 */
public class MethodModel {
	private String path;
	private String name;
	private String description;
	private Operation operation; // CRUD operation that is being performed
	private List<ParameterModel> parameters; // List of the parameters accepted by the method.
	private RequestBodyModel requestBody;
	private ResponseModel response;
	private Boolean authenticationRequired;
	
	public String getPath() {
		return path;
	}
	
	public MethodModel withPath(String path) {
		this.path = path;
		return this;
	}
	
	public Operation getOperation() {
		return operation;
	}
	
	public MethodModel withOperation(Operation operation) {
		this.operation = operation;
		return this;
	}
	
	public List<ParameterModel> getParameters() {
		return parameters;
	}
	
	public MethodModel withParameters(List<ParameterModel> parameters) {
		this.parameters = parameters;
		return this;
	}
	
	public RequestBodyModel getRequestBody() {
		return requestBody;
	}
	
	public MethodModel withRequestBody(RequestBodyModel requestBody) {
		this.requestBody = requestBody;
		return this;
	}
	
	public ResponseModel getResponse() {
		return response;
	}
	
	public MethodModel withResponse(ResponseModel response) {
		this.response = response;
		return this;
	}
	
	public String getName() {
		return name;
	}

	public MethodModel withName(String name) {
		this.name = name;
		return this;
	}
	
	public String getDescription() {
		return description;
	}

	public MethodModel withDescription(String description) {
		this.description = description;
		return this;
	}

	public Boolean getAuthenticationRequired() {
		return authenticationRequired;
	}

	public MethodModel withAuthenticationRequired(Boolean authenticationRequired) {
		this.authenticationRequired = authenticationRequired;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, operation, parameters, path, requestBody, response, authenticationRequired);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodModel other = (MethodModel) obj;
		return Objects.equals(name, other.name) && operation == other.operation
				&& Objects.equals(parameters, other.parameters) && Objects.equals(path, other.path)
				&& Objects.equals(requestBody, other.requestBody) && Objects.equals(response, other.response)
				&& Objects.equals(authenticationRequired, other.authenticationRequired);
	}

	@Override
	public String toString() {
		return "MethodModel [path=" + path + ", name=" + name + ", operation=" + operation + ", parameters="
				+ parameters + ", requestBody=" + requestBody + ", response=" + response + ", authenticationRequired=" + authenticationRequired + "]";
	}
}
