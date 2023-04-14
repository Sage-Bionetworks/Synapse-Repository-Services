package org.sagebionetworks.controller.annotations.model;

import java.util.Objects;

import org.springframework.http.HttpStatus;

public class ResponseStatusModel {
	private HttpStatus status;

	public HttpStatus getStatus() {
		return status;
	}

	public ResponseStatusModel withStatus(HttpStatus status) {
		this.status = status;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(status);
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
		return status == other.status;
	}

	@Override
	public String toString() {
		return "ResponseStatusModel [status=" + status + "]";
	}
}
