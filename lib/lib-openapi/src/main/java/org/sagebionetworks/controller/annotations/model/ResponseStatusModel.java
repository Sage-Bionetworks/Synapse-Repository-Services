package org.sagebionetworks.controller.annotations.model;

import java.util.Objects;

public class ResponseStatusModel {
	private Integer statusCode;

	public Integer getStatusCode() {
		return statusCode;
	}

	public ResponseStatusModel withStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(statusCode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResponseStatusModel other = (ResponseStatusModel) obj;
		return Objects.equals(statusCode, other.statusCode);
	}

	@Override
	public String toString() {
		return "ResponseStatusModel [statusCode=" + statusCode + "]";
	}
}
