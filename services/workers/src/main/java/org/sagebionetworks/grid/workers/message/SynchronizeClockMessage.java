package org.sagebionetworks.grid.workers.message;

import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;

public class SynchronizeClockMessage implements RequestDataMessage {

	private final EventContext context;
	private final Integer requestId;
	private final List<LogicalTimestamp> clock;

	public SynchronizeClockMessage(EventContext context, Integer requestId, JSONArray body) {
		super();
		this.context = context;
		this.requestId = requestId;
		this.clock = LogicalTimestampCompactSerializable.deserializeClock(body);
	}

	public EventContext getContext() {
		return context;
	}

	public Integer getRequestId() {
		return requestId;
	}

	public List<LogicalTimestamp> getClock() {
		return clock;
	}

	@Override
	public int hashCode() {
		return Objects.hash(clock, context, requestId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SynchronizeClockMessage other = (SynchronizeClockMessage) obj;
		return Objects.equals(clock, other.clock) && Objects.equals(context, other.context)
				&& Objects.equals(requestId, other.requestId);
	}

	@Override
	public String toString() {
		return "SynchronizeClockMessage [context=" + context + ", requestId=" + requestId + ", clock=" + clock + "]";
	}

}
