package org.sagebionetworks.repo.model.grid;

import java.util.Objects;

import org.json.JSONObject;

/**
 * See:
 * <a href="https://jsonjoy.com/specs/json-rx/json-encoding#Errors">Errors</a>
 */
public class ErrorEvent {

	// - A human-readable error message.
	private String message;
	// (optional, ASCII string) - An error code, which is a short stable error ID,
	// intended for programmatic error handling.
	private String code;
	// (optional, integer) - Same as code but in numeric form. An error number,
	// which is a stable error ID, intended for programmatic error handling.
	private Integer errno;
	// (optional, string) - A unique error ID, which was used to store the error in
	// the server-side error log. Later the error can be referenced by this ID.
	private String errorId;
	// (optional, JSON value) - Additional error metadata, such as stack trace, etc.
	private JSONObject meta;

	public String getMessage() {
		return message;
	}

	public ErrorEvent setMessage(String message) {
		this.message = message;
		return this;
	}

	public String getCode() {
		return code;
	}

	public Integer getErrno() {
		return errno;
	}

	public ErrorEvent setError(ErrorType error) {
		this.code = error.getCode();
		this.errno = error.getErrno();
		return this;
	}

	public String getErrorId() {
		return errorId;
	}

	public void setErrorId(String errorId) {
		this.errorId = errorId;
	}

	public JSONObject getMeta() {
		return meta;
	}

	public void setMeta(JSONObject meta) {
		this.meta = meta;
	}

	public JSONObject toJsonObject() {
		JSONObject json = new JSONObject();
		if (message != null) {
			json.put("message", message);
		}
		if (code != null) {
			json.put("code", code);
		}
		if (errno != null) {
			json.put("errno", errno);
		}
		if (errorId != null) {
			json.put("errorId", errorId);
		}
		if (meta != null) {
			json.put("meta", meta);
		}
		return json;
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, errno, errorId, message, meta);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ErrorEvent other = (ErrorEvent) obj;
		return Objects.equals(code, other.code) && Objects.equals(errno, other.errno)
				&& Objects.equals(errorId, other.errorId) && Objects.equals(message, other.message)
				&& Objects.equals(meta, other.meta);
	}

	@Override
	public String toString() {
		return toJsonObject().toString();
	}

}
