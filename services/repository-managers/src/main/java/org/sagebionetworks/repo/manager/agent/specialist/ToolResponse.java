package org.sagebionetworks.repo.manager.agent.specialist;

import java.util.Objects;

import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * A tool response can either be a JSONEntity or an error message.
 * @param <T>
 */
public class ToolResponse<T extends JSONEntity> implements JSONEntity {

	private final T responseBody;
	private final String errorMessage;

	public ToolResponse(T responseBody) {
		super();
		this.responseBody = responseBody;
		this.errorMessage = null;
	}

	public ToolResponse(String errorMessage) {
		super();
		this.responseBody = null;
		this.errorMessage = errorMessage;
	}

	public T getResponseBody() {
		return responseBody;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {
		if (this.responseBody != null) {
			writeTo.put("responseBody", this.responseBody.writeToJSONObject(writeTo.createNew()));
		}
		if (this.errorMessage != null) {
			writeTo.put("errorMessage", errorMessage);
		}
		return writeTo;
	}

	@Override
	public int hashCode() {
		return Objects.hash(errorMessage, responseBody);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ToolResponse other = (ToolResponse) obj;
		return Objects.equals(errorMessage, other.errorMessage) && Objects.equals(responseBody, other.responseBody);
	}

}
