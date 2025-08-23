package org.sagebionetworks.grid.db;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Track information about an on-going JsonRxMessage chain.
 */
public class MessageChain {

	private String sessionId;
	private Long replicaId;
	private Integer id;
	private String method;
	private Timestamp createdOn;

	public String getSessionId() {
		return sessionId;
	}

	public MessageChain setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public Long getReplicaId() {
		return replicaId;
	}

	public MessageChain setReplicaId(Long replicaId) {
		this.replicaId = replicaId;
		return this;
	}

	public Integer getId() {
		return id;
	}

	public MessageChain setId(Integer id) {
		this.id = id;
		return this;
	}

	public String getMethod() {
		return method;
	}

	public MessageChain setMethod(String method) {
		this.method = method;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public MessageChain setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, id, method, replicaId, sessionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageChain other = (MessageChain) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(id, other.id)
				&& Objects.equals(method, other.method) && Objects.equals(replicaId, other.replicaId)
				&& Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return "MessageChain [sessionId=" + sessionId + ", replicaId=" + replicaId + ", id=" + id + ", method=" + method
				+ ", createdOn=" + createdOn + "]";
	}

}
