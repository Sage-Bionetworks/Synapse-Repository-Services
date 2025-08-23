package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;

public class JsonRxMessageBundle {

	private final JsonRxMessage message;
	private final GridConnectionInfo connection;
	private final ProgressCallback progressCallback;

	public JsonRxMessageBundle(JsonRxMessage message, GridConnectionInfo connection,
			ProgressCallback progressCallback) {
		super();
		ValidateArgument.required(message, "message");
		ValidateArgument.required(connection, "connection");
		ValidateArgument.required(progressCallback, "progressCallback");
		this.message = message;
		this.connection = connection;
		this.progressCallback = progressCallback;
	}

	public JsonRxMessage getMessage() {
		return message;
	}

	public GridConnectionInfo getConnection() {
		return connection;
	}

	public ProgressCallback getProgressCallback() {
		return progressCallback;
	}

	@Override
	public int hashCode() {
		return Objects.hash(connection, message, progressCallback);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonRxMessageBundle other = (JsonRxMessageBundle) obj;
		return Objects.equals(connection, other.connection) && Objects.equals(message, other.message)
				&& Objects.equals(progressCallback, other.progressCallback);
	}

	@Override
	public String toString() {
		return "JsonRxMessageBundle [message=" + message + ", connection=" + connection + ", progressCallback="
				+ progressCallback + "]";
	}

}
