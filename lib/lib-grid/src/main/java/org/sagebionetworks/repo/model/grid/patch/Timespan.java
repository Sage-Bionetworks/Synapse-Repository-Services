package org.sagebionetworks.repo.model.grid.patch;

import java.util.Objects;

/**
 * A timespan represents an interval of logical timestamps. 
 * 
 * See https://jsonjoy.com/specs/json-crdt-patch/encoding/compact-format#del-Operation-Encoding
 */
public class Timespan {

	private LogicalTimestamp start;
	private Long length;
	
	public Timespan(LogicalTimestamp start, Long length) {
		this.start = start;
		this.length = length;
	}
	
	public LogicalTimestamp getStart() {
		return start;
	}
	
	public Long getLength() {
		return length;
	}

	@Override
	public int hashCode() {
		return Objects.hash(length, start);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Timespan)) {
			return false;
		}
		Timespan other = (Timespan) obj;
		return Objects.equals(length, other.length) && Objects.equals(start, other.start);
	}

	@Override
	public String toString() {
		return String.format("Timespan [start=%s, length=%s]", start, length);
	}

}
