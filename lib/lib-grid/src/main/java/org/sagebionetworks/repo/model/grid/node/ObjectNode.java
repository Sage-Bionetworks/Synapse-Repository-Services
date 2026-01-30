package org.sagebionetworks.repo.model.grid.node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;
import org.sagebionetworks.util.ValidateArgument;

public class ObjectNode implements Node, HasJsonValue<ObjectNode>, CanInsert<InsertObject> {

	private LogicalTimestamp id;
	private Map<String, LogicalTimestamp> value;

	@Override
	public LogicalTimestamp getId() {
		return id;
	}

	public Map<String, LogicalTimestamp> getValue() {
		return value;
	}

	public ObjectNode setValue(Map<String, LogicalTimestamp> map) {
		this.value = map;
		return this;
	}

	public ObjectNode setId(LogicalTimestamp id) {
		this.id = id;
		return this;
	}

	/**
	 * Set the value from the passed JSON string.
	 * 
	 * @param json
	 * @return
	 */
	public ObjectNode setValueFromJson(String json) {
		if ("{}".equals(json)) {
			this.value = null;
			return this;
		}
		JSONObject obj = new JSONObject(json);
		this.value = new LinkedHashMap<>(obj.length());
		obj.keySet().stream().forEach(k -> {
			value.put(k, LogicalTimestampCompactSerializable.deserialize(obj.getJSONArray(k)));
		});
		return this;
	}

	/**
	 * Get the JSON representation of the value.
	 * 
	 * @return
	 */
	public String getValueAsJson() {
		if (value == null) {
			return "{}";
		}
		JSONObject ob = new JSONObject();
		value.forEach((k, v) -> {
			ob.put(k, LogicalTimestampCompactSerializable.serialize(v));
		});
		return ob.toString();
	}

	/**
	 * Attempt to insert the {@link InsertObject} following last-writer-wins (LWW)
	 * for each new value.
	 * 
	 * @param change
	 * @return True if this object was updated, otherwise false.
	 */
	@Override
	public boolean attemptInsert(InsertObject change) {
		ValidateArgument.required(change, "change");
		ValidateArgument.required(change.getObjectId(), "change.id");
		if (!change.getObjectId().equals(this.id)) {
			throw new IllegalArgumentException("The ID of the passed change does not match the ID of this object.");
		}
		if (change.getMap() == null) {
			return false;
		}
		if (this.value == null) {
			this.value = new LinkedHashMap<>();
		}
		boolean wasChanged = false;
		for (Map.Entry<String, LogicalTimestamp> entry : change.getMap().entrySet()) {
			String key = entry.getKey();
			LogicalTimestamp changeId = entry.getValue();
			if (changeId == null) {
				throw new IllegalArgumentException("Cannot set an object value to null");
			}
			// The ID of the new value must be greater than the container node (to avoid circular references), otherwise the insertion is ignored (See https://sagebionetworks.jira.com/browse/PLFM-9273).
			if (changeId.compareTo(this.id) <= 0) {
				continue;
			}
			LogicalTimestamp thisId = this.value.get(key);
			// The ID of the new value must be greater than the logical clock of the current value, otherwise the insertion is ignored.
			if (thisId == null || changeId.compareTo(thisId) > 0) {
				this.value.put(key, LogicalTimestamp.clone(changeId));
				wasChanged = true;
			}
		}
		return wasChanged;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectNode other = (ObjectNode) obj;
		return Objects.equals(id, other.id) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "ObjectNode [id=" + id + ", map=" + value + "]";
	}

}
