package org.sagebionetworks.grid.workers.message;

import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.internal.Connection;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class ConnectionMessage implements NotificationMessage {

	private final EventContext context;
	private final Connection connection;

	public ConnectionMessage(EventContext context, JSONObject body) {
		this.context = context;
		try {
			this.connection = EntityFactory.createEntityFromJSONObject(body, Connection.class);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}

	public EventContext getContext() {
		return context;
	}

	public Connection getConnection() {
		return connection;
	}

	@Override
	public int hashCode() {
		return Objects.hash(connection, context);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConnectionMessage other = (ConnectionMessage) obj;
		return Objects.equals(connection, other.connection) && Objects.equals(context, other.context);
	}

	@Override
	public String toString() {
		return "ConnectionMessage [context=" + context + ", connection=" + connection + "]";
	}

}