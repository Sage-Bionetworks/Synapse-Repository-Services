package org.sagebionetworks.repo.model.grid;

import java.util.Objects;

import org.json.JSONArray;

public class ResponseError {

	private int subscriptionId = -1;
	private ErrorEvent event;

	public JSONArray toJSONArray() {
		JSONArray array = new JSONArray();
		array.put(0, 6);
		array.put(1, subscriptionId);
		array.put(2, event.toJsonObject());
		return array;
	}

	public int getSubscriptionId() {
		return subscriptionId;
	}

	public ResponseError setSubscriptionId(int subscriptionId) {
		this.subscriptionId = subscriptionId;
		return this;
	}

	public ErrorEvent getEvent() {
		return event;
	}

	public ResponseError setEvent(ErrorEvent event) {
		this.event = event;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(event, subscriptionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResponseError other = (ResponseError) obj;
		return Objects.equals(event, other.event) && subscriptionId == other.subscriptionId;
	}

	@Override
	public String toString() {
		return toJSONArray().toString();
	}

}
