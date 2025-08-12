package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;

/**
 * Captures a set of changes to nodes that occurred when a patch was applied to
 * a replica. Can be serialized/deserialized a compact JSON representation.
 */
public class ReplicaChangeSet {

	private final String connectionId;
	private final String sessionId;
	private final Long replicaId;
	private final LogicalTimestamp patchId;
	private final Map<IndexType, Set<LogicalTimestamp>> changes;

	public ReplicaChangeSet(GridConnectionInfo connection, LogicalTimestamp patchId,
			Map<IndexType, Set<LogicalTimestamp>> changes) {
		this.connectionId = connection.getConnectionId();
		this.sessionId = connection.getSessionId();
		this.replicaId = connection.getReplicaId();
		this.patchId = patchId;
		this.changes = changes;
	}

	public ReplicaChangeSet(String jsonString) {
		this(new JSONObject(jsonString));
	}

	public ReplicaChangeSet(JSONObject json) {
		this.connectionId = json.getString("connectionId");
		this.sessionId = json.getString("sessionId");
		this.replicaId = json.getLong("replicaId");
		this.patchId = LogicalTimestampCompactSerializable.deserialize(json.getJSONArray("patchId"));
		JSONObject changeObj = json.optJSONObject("changes");
		this.changes = changeObj != null ? new LinkedHashMap<>() : null;
		if (changes != null) {
			changeObj.keySet().stream().forEach(k -> {
				IndexType type = IndexType.valueOf(k);
				JSONArray array = changeObj.getJSONArray(k);
				LinkedHashSet<LogicalTimestamp> set = new LinkedHashSet<>();
				for (int i = 0; i < array.length(); i++) {
					set.add(LogicalTimestampCompactSerializable.deserialize(patchId.getReplicaId(), array, i));
				}
				changes.put(type, set);
			});
		}
	}

	public String toJson() {
		JSONObject json = new JSONObject();
		json.put("connectionId", connectionId);
		json.put("sessionId", sessionId);
		json.put("replicaId", replicaId);
		json.put("patchId", LogicalTimestampCompactSerializable.serialize(patchId));
		JSONObject changesObj = new JSONObject();
		if (changes != null) {
			changes.forEach((k, s) -> {
				JSONArray subChanges = new JSONArray();
				s.forEach(l -> {
					subChanges.put(LogicalTimestampCompactSerializable.serialize(patchId.getReplicaId(), l));
				});
				changesObj.put(k.name(), subChanges);
			});
			json.put("changes", changesObj);
		}
		return json.toString();
	}

	public String getConnectionId() {
		return connectionId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public Long getReplicaId() {
		return replicaId;
	}

	public Map<IndexType, Set<LogicalTimestamp>> getChanges() {
		return changes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(changes, connectionId, replicaId, sessionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReplicaChangeSet other = (ReplicaChangeSet) obj;
		return Objects.equals(changes, other.changes) && Objects.equals(connectionId, other.connectionId)
				&& Objects.equals(replicaId, other.replicaId) && Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return toJson();
	}

}
