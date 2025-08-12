package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.List;
import java.util.Objects;

public class IntendedChangeSet {

	private String sessionId;
	private Long replicaId;
	private String connectionId;
	private List<IntendedChange> changes;

	public String getSessionId() {
		return sessionId;
	}

	public IntendedChangeSet setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public Long getReplicaId() {
		return replicaId;
	}

	public IntendedChangeSet setReplicaId(Long replicaId) {
		this.replicaId = replicaId;
		return this;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public IntendedChangeSet setConnectionId(String connectionId) {
		this.connectionId = connectionId;
		return this;
	}

	public List<IntendedChange> getChanges() {
		return changes;
	}

	public IntendedChangeSet setChanges(List<IntendedChange> changes) {
		this.changes = changes;
		return this;
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
		IntendedChangeSet other = (IntendedChangeSet) obj;
		return Objects.equals(changes, other.changes) && Objects.equals(connectionId, other.connectionId)
				&& Objects.equals(replicaId, other.replicaId) && Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return "IntendedChangeSet [sessionId=" + sessionId + ", replicaId=" + replicaId + ", connectionId="
				+ connectionId + ", changes=" + changes + "]";
	}

}
