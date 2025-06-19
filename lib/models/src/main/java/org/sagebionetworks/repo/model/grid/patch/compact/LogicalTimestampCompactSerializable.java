package org.sagebionetworks.repo.model.grid.patch.compact;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Reads and writes LogicalTimestamps as compact JSON.
 */
public class LogicalTimestampCompactSerializable {

	/**
	 * Deserialize a LogicalTimestamp.
	 * 
	 * @param array
	 * @return
	 */
	public static LogicalTimestamp deserialize(JSONArray array) {
		return new LogicalTimestamp().setReplicaId(array.getLong(0)).setSequenceNumber(array.getLong(1));
	}

	/**
	 * When a LogicalTimestamp has the same repliaId as the patch, the replicaId can
	 * be encoded using only the sequence number.
	 * 
	 * @param replicaId The replica ID of the patch.
	 * @param parent    The array that contains the timestamp to read.
	 * @param index     The index of the timestamp in the array.
	 * @return
	 */
	public static LogicalTimestamp deserialize(Long replicaId, JSONArray parent, int index) {
		JSONArray value = parent.optJSONArray(index);
		if (value != null) {
			return deserialize(value);
		} else {
			// The replica was excluded since it matches the id's replica
			long longValue = parent.getLong(index);
			return new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(longValue);
		}
	}

	/**
	 * When a LogicalTimestamp has the same repliaId as the patch, the replicaId can
	 * be encoded using only the sequence number.
	 * 
	 * @param replicaId The replica ID of the patch.
	 * @param value The timestamp to encode.
	 * @return
	 */
	public static Object serialize(Long replicaId, LogicalTimestamp value) {
		if (replicaId.equals(value.getReplicaId())) {
			return value.getSequenceNumber();
		} else {
			return serialize(value);
		}
	}

	/**
	 * Serialize a LogicalTimestamp;
	 * 
	 * @param time
	 * @return
	 */
	public static JSONArray serialize(LogicalTimestamp time) {
		return new JSONArray().put(0, time.getReplicaId()).put(1, time.getSequenceNumber());
	}

	/**
	 * Deserialize a JSON array that represented a clock (version vector).
	 * 
	 * @param array
	 * @return
	 */
	public static List<LogicalTimestamp> deserializeClock(JSONArray array) {
		ValidateArgument.required(array, "array");
		List<LogicalTimestamp> clock = new ArrayList<>(array.length());
		for (int i = 0; i < array.length(); i++) {
			JSONArray idArray = array.optJSONArray(i);
			if (idArray != null) {
				clock.add(deserialize(idArray));
			}
		}
		return clock;
	}

	/**
	 * Serialize a clock (version vector) to JSON Array.
	 * 
	 * @param clock
	 * @return
	 */
	public static JSONArray serializeClock(List<LogicalTimestamp> clock) {
		ValidateArgument.required(clock, "clock");
		JSONArray array = new JSONArray();
		clock.forEach(t -> {
			array.put(serialize(t));
		});
		return array;
	}
}
