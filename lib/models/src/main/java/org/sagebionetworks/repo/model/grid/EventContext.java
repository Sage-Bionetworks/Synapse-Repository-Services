package org.sagebionetworks.repo.model.grid;

import java.util.Objects;

/**
 * Abstraction for the context of a grid event.
 */
public class EventContext {

	private final EventType eventType;
	private final EventSource eventSource;
	private final String connectionId;

	public EventContext(EventType eventType, EventSource eventSource, String connectionId) {
		super();
		this.eventType = eventType;
		this.eventSource = eventSource;
		this.connectionId = connectionId;
	}

	public EventType getEventType() {
		return eventType;
	}

	public EventSource getEventSource() {
		return eventSource;
	}

	public String getConnectionId() {
		return connectionId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectionId, eventSource, eventType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventContext other = (EventContext) obj;
		return Objects.equals(connectionId, other.connectionId) && eventSource == other.eventSource
				&& eventType == other.eventType;
	}

	@Override
	public String toString() {
		return "EventContext [eventType=" + eventType + ", eventSource=" + eventSource + ", connectionId="
				+ connectionId + "]";
	}

}
