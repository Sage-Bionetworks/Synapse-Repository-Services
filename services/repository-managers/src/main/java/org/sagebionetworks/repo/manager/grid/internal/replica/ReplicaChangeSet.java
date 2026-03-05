package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.util.Collections;
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
 * Captures a set of changes to nodes that occurred when a patch or snapshot was
 * applied to a replica. Can be serialized/deserialized a compact JSON representation.
 */
public class ReplicaChangeSet {

	public enum ChangeSource {
		PATCH,
		SNAPSHOT
	}

	private final String sessionId;
	private final Long replicaId;
	private final ChangeSource changeSource;
	private final Map<IndexType, Set<LogicalTimestamp>> changes;

	private ReplicaChangeSet(String sessionId, Long replicaId, Map<IndexType, Set<LogicalTimestamp>> changes, ChangeSource changeSource) {
		this.sessionId = sessionId;
		this.replicaId = replicaId;
		this.changes = changes;
		this.changeSource = changeSource;
	}

	public static ReplicaChangeSet fromPatch(GridConnectionInfo connection, Map<IndexType, Set<LogicalTimestamp>> changes) {
		return new ReplicaChangeSet(connection.getSessionId(), connection.getReplicaId(), changes,	ChangeSource.PATCH);
	}

	public static ReplicaChangeSet fromSnapshot(GridConnectionInfo connection) {
		return new ReplicaChangeSet(connection.getSessionId(), connection.getReplicaId(), Collections.emptyMap(), ChangeSource.SNAPSHOT);
	}

	public ReplicaChangeSet(String jsonString) {
		this(new JSONObject(jsonString));
	}

	public ReplicaChangeSet(JSONObject json) {
		this.sessionId = json.getString("sessionId");
		this.replicaId = json.getLong("replicaId");
		String changeSourceStr = json.optString("changeSource", null);
		this.changeSource = changeSourceStr != null ? ChangeSource.valueOf(changeSourceStr) : null;
		JSONObject changeObj = json.optJSONObject("changes");
		this.changes = changeObj != null ? new LinkedHashMap<>() : null;
		if (changes != null) {
			changeObj.keySet().stream().forEach(k -> {
				IndexType type = IndexType.valueOf(k);
				JSONArray array = changeObj.getJSONArray(k);
				LinkedHashSet<LogicalTimestamp> set = new LinkedHashSet<>();
				for (int i = 0; i < array.length(); i++) {
					set.add(LogicalTimestampCompactSerializable.deserialize(array.getJSONArray(i)));
				}
				changes.put(type, set);
			});
		}
	}

	public String toJson() {
		JSONObject json = new JSONObject();
		json.put("sessionId", sessionId);
		json.put("replicaId", replicaId);
		json.put("changeSource", changeSource.name());
		if (changes != null) {
			JSONObject changesObj = new JSONObject();
			changes.forEach((k, s) -> {
				JSONArray subChanges = new JSONArray();
				s.forEach(l -> subChanges.put(LogicalTimestampCompactSerializable.serialize(l)));
				changesObj.put(k.name(), subChanges);
			});
			json.put("changes", changesObj);
		}
		return json.toString();
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

	public ChangeSource getChangeSource() {
		return changeSource;
	}

	@Override
	public int hashCode() {
		return Objects.hash(changes, changeSource, replicaId, sessionId);
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
		return Objects.equals(changes, other.changes) && changeSource == other.changeSource
				&& Objects.equals(replicaId, other.replicaId) && Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return toJson();
	}

}
