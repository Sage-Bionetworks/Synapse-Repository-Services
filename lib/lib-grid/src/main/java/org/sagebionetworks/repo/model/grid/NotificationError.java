package org.sagebionetworks.repo.model.grid;

import java.util.Objects;

import org.json.JSONArray;

public class NotificationError {

	private ErrorEvent event;

	public JSONArray toJSONArray() {
		JSONArray array = new JSONArray();
		array.put(0, 8);
		array.put(1, "error");
		array.put(2, event.toJsonObject());
		return array;
	}

	public ErrorEvent getEvent() {
		return event;
	}

	public NotificationError setEvent(ErrorEvent event) {
		this.event = event;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(event);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NotificationError other = (NotificationError) obj;
		return Objects.equals(event, other.event);
	}

	@Override
	public String toString() {
		return toJSONArray().toString();
	}

}
