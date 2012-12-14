package org.sagebionetworks.logging.collate;

import org.joda.time.DateTime;

public final class LogEvent implements Comparable<LogEvent> {

	private final DateTime timestamp;
	private final String line;

	public LogEvent(DateTime timestamp) {
		if (timestamp == null) throw new IllegalArgumentException("Timestamp cannot be null.");
		this.timestamp = timestamp;
		this.line = null;
	}

	/**
	 * Construct an immutable LogEvent
	 * @param timestamp
	 *            - required parameter
	 * @param line
	 *            - Not required. If present, then line should contain the
	 *            entire log event (including the leading timestamp)
	 */
	public LogEvent(DateTime timestamp, String line) {
		if (timestamp == null) throw new IllegalArgumentException("Timestamp cannot be null.");
		this.timestamp = timestamp;
		this.line = line;
	}

	public DateTime getTimestamp() {
		return timestamp;
	}

	public String getLine() {
		return line;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (!(o instanceof LogEvent)) {
			return false;
		}

		LogEvent le = (LogEvent) o;
		if (this.compareTo(le) == 0)
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {
		int result = 42;
		result = 31 * result + timestamp.hashCode();
		result = 31 * result + (line != null ? line.hashCode() : 0);
		return result;
	}

	@Override
	public int compareTo(LogEvent le) {
		int compareTo = timestamp.compareTo(le.timestamp);

		if (compareTo == 0 && line != null)
			compareTo = line.compareTo(le.line);
		else if (compareTo == 0 && le.line != null)
			compareTo = -1;

		return compareTo;
	}

	@Override
	public String toString() {
		return line;
	}

}
