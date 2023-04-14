package org.sagebionetworks.controller.annotations.model;

import java.util.Objects;

import org.springframework.web.bind.annotation.RequestMethod;

public class RequestMappingModel {
	private String path;
	private RequestMethod operation;
	
	public String getPath() {
		return path;
	}
	
	public RequestMappingModel withPath(String path) {
		this.path = path;
		return this;
	}
	
	public RequestMethod getOperation() {
		return operation;
	}
	
	public RequestMappingModel withOperation(RequestMethod operation) {
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
