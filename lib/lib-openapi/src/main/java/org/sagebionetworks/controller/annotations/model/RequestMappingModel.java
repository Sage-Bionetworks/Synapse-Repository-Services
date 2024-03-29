package org.sagebionetworks.controller.annotations.model;

import java.util.Objects;

import org.sagebionetworks.controller.model.Operation;

public class RequestMappingModel {
	private String path;
	private Operation operation;
	
	public String getPath() {
		return path;
	}
	
	public RequestMappingModel withPath(String path) {
		this.path = path;
		return this;
	}
	
	public Operation getOperation() {
		return operation;
	}
	
	public RequestMappingModel withOperation(Operation operation) {
		this.operation = operation;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(operation, path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RequestMappingModel other = (RequestMappingModel) obj;
		return operation == other.operation && Objects.equals(path, other.path);
	}

	@Override
	public String toString() {
		return "RequestMappingModel [path=" + path + ", operation=" + operation + "]";
	}
	
}
