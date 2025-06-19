package org.sagebionetworks.repo.model.grid.patch;

import java.util.Objects;

public class LogicalTimestamp {

	private Long replicaId;
	private Long sequenceNumber;

	public Long getReplicaId() {
		return replicaId;
	}

	public LogicalTimestamp setReplicaId(Long replicaId) {
		this.replicaId = replicaId;
		return this;
	}

	public Long getSequenceNumber() {
		return sequenceNumber;
	}

	public LogicalTimestamp setSequenceNumber(Long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
		return this;
	}

	/**
	 * Create a new LogicalTimestamp with a sequence that is incremented by the
	 * provided span.
	 * 
	 * @param original
	 * @param span
	 * @return
	 */
	public static LogicalTimestamp newIncrement(LogicalTimestamp original, long span) {
		return new LogicalTimestamp().setReplicaId(original.getReplicaId())
				.setSequenceNumber(original.getSequenceNumber() + span);
	}

	/**
	 * Create a clone of the provided LogicalTimestamp.
	 * @param original
	 * @return
	 */
	public static LogicalTimestamp clone(LogicalTimestamp original) {
		return new LogicalTimestamp().setReplicaId(original.getReplicaId())
				.setSequenceNumber(original.getSequenceNumber());
	}

	@Override
	public int hashCode() {
		return Objects.hash(replicaId, sequenceNumber);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LogicalTimestamp other = (LogicalTimestamp) obj;
		return Objects.equals(replicaId, other.replicaId) && Objects.equals(sequenceNumber, other.sequenceNumber);
	}

	@Override
	public String toString() {
		return "LogicalTimestamp [replicaId=" + replicaId + ", sequenceNumber=" + sequenceNumber + "]";
	}

}
