package org.sagebionetworks.repo.model.grid;

import java.util.Date;
import java.util.Objects;

/**
 * Information captured for each grid replica connection established.
 */
public class GridConnectionInfo {

	private String connectionId;
	private String sessionId;
	private Long replicaId;
	private Long createdBy;
	private Date createdOn;
	private EventSource source;

	public String getConnectionId() {
		return connectionId;
	}

	public GridConnectionInfo setConnectionId(String connectionId) {
		this.connectionId = connectionId;
		return this;
	}

	public String getSessionId() {
		return sessionId;
	}

	public GridConnectionInfo setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public Long getReplicaId() {
		return replicaId;
	}

	public GridConnectionInfo setReplicaId(Long replicaId) {
		this.replicaId = replicaId;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public GridConnectionInfo setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public GridConnectionInfo setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public EventSource getSource() {
		return source;
	}

	public GridConnectionInfo setSource(EventSource source) {
		this.source = source;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectionId, createdBy, createdOn, replicaId, sessionId, source);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GridConnectionInfo other = (GridConnectionInfo) obj;
		return Objects.equals(connectionId, other.connectionId) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(replicaId, other.replicaId)
				&& Objects.equals(sessionId, other.sessionId) && source == other.source;
	}

	@Override
	public String toString() {
		return "ConnectionInfo [connectionId=" + connectionId + ", sessionId=" + sessionId + ", replicaId=" + replicaId
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", source=" + source + "]";
	}

}
