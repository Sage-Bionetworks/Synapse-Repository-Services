package org.sagebionetworks.repo.model.grid.patch;

import java.util.Objects;

import org.sagebionetworks.util.ValidateArgument;

public class LogicalTimestamp implements Comparable<LogicalTimestamp> {

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
	 * 
	 * @param original
	 * @return
	 */
	public static LogicalTimestamp clone(LogicalTimestamp original) {
		return new LogicalTimestamp().setReplicaId(original.getReplicaId())
				.setSequenceNumber(original.getSequenceNumber());
	}
	
	/**
	 * Parse a compact string of the form replicaId.sequenceNumber into a LogicalTimestamp.
	 * @param value the string (e.g. "123.456")
	 * @return LogicalTimestamp
	 * @throws IllegalArgumentException if the format or numbers are invalid
	 */
	public static LogicalTimestamp parse(String value) {
		ValidateArgument.required(value, "value");
		String v = value.trim();
		int sep = v.indexOf('.');
		if (sep < 1 || sep == v.length() - 1) {
			throw new IllegalArgumentException("Expected format 'replicaId.sequenceNumber'");
		}
		String replicaPart = v.substring(0, sep);
		String seqPart = v.substring(sep + 1);
		try {
			Long replica = Long.parseLong(replicaPart);
			Long seq = Long.parseLong(seqPart);
			return new LogicalTimestamp().setReplicaId(replica).setSequenceNumber(seq);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid number in '" + value + "'", e);
		}
	}
	
	/**
	 * Return the compact form `replicaId.sequenceNumber`.
	 */
	public String toCompact() {
		ValidateArgument.required(replicaId, "replicaId");
		ValidateArgument.required(sequenceNumber, "sequenceNumber");
		return replicaId + "." + sequenceNumber;
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

	@Override
	public int compareTo(LogicalTimestamp o) {
		ValidateArgument.required(o, "other");
		ValidateArgument.required(o.getReplicaId(), "other.replicaId");
		ValidateArgument.required(o.getSequenceNumber(), "other.sequenceNumber");
		ValidateArgument.required(this.replicaId, "this.replicaId");
		ValidateArgument.required(this.sequenceNumber, "this.sequenceNumber");
		int seqComp = this.sequenceNumber.compareTo(o.sequenceNumber);
		if (seqComp != 0) {
			return seqComp;
		}
		return this.replicaId.compareTo(o.replicaId);
	}

}
