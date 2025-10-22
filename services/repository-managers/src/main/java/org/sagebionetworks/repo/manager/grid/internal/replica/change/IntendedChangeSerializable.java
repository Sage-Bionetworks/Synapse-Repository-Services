package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.util.ValidateArgument;

public class IntendedChangeSerializable {

	/**
	 * Serialize the passed changes into a JSONArray.
	 * 
	 * @param changes
	 * @return
	 */
	public static JSONArray serialize(List<IntendedChange> changes) {
		ValidateArgument.required(changes, "changes");
		JSONArray array = new JSONArray();
		changes.forEach(c -> {
			JSONArray change = new JSONArray();
			change.put(c.getType().getCode());
			change.put(c.toJson());
			array.put(change);
		});
		return array;
	}

	/**
	 * Serialize a change set to JSON.
	 * 
	 * @param set
	 * @return
	 */
	public static JSONObject serialize(IntendedChangeSet set) {
		ValidateArgument.required(set, "IntendedChangeSet");
		ValidateArgument.required(set.getConnectionId(), "set.connectionId");
		ValidateArgument.required(set.getReplicaId(), "set.replicaId");
		ValidateArgument.required(set.getSessionId(), "set.sessionId");
		ValidateArgument.required(set.getClockSequenceMaximum(), "set.clockSequenceMaximum");
		JSONObject json = new JSONObject();
		json.put("con", set.getConnectionId());
		json.put("ses", set.getSessionId());
		json.put("rep", set.getReplicaId());
		json.put("max", set.getClockSequenceMaximum());
		json.put("set", serialize(set.getChanges()));
		return json;
	}

	/**
	 * Deserialize the passed JSONArray into a list of changes.
	 * 
	 * @param array
	 * @return
	 */
	public static List<IntendedChange> deserialize(JSONArray array) {
		ValidateArgument.required(array, "array");
		List<IntendedChange> list = new ArrayList<>(array.length());
		for (int i = 0; i < array.length(); i++) {
			JSONArray sub = array.getJSONArray(i);
			IntendedChangeType type = IntendedChangeType.fromCode(sub.getInt(0));
			switch (type) {
			case update_row_metadata:
				list.add(new UpdateMetadataChange(sub.getJSONObject(1)));
				break;
			case insert_row:
				list.add(new InsertRowChange(sub.getJSONObject(1)));
				break;
			case update_row:
				list.add(new UpdateRowChange(sub.getJSONObject(1)));
				break;
			default:
				throw new IllegalArgumentException("Unknown type:" + type);
			}
		}
		return list;
	}

	/**
	 * Deserialize the passed JSON object into a change set.
	 * 
	 * @param json
	 * @return
	 */
	public static IntendedChangeSet deserialize(JSONObject json) {
		ValidateArgument.required(json, "array");
		return new IntendedChangeSet().setSessionId(json.getString("ses")).setReplicaId(json.getLong("rep"))
				.setConnectionId(json.getString("con")).setClockSequenceMaximum(json.getLong("max"))
				.setChanges(deserialize(json.getJSONArray("set")));
	}

}
