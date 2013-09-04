package org.sagebionetworks.logging.s3;

import java.text.ParseException;

/**
 * A single log entry includes the string of the entry and the time stamp.
 * 
 * @author John
 *
 */
public class LogEntry implements Comparable<LogEntry> {

	long timeStamp;
	StringBuilder buffer;
	
	/**
	 * The first line of an entry must start with a ISO8601 GMT String.
	 * @param firstLine
	 * @throws ParseException
	 */
	public LogEntry(String firstLine) throws ParseException{
		timeStamp = LogKeyUtils.readISO8601GMTFromString(firstLine);
		buffer = new StringBuilder(firstLine);
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	public String getEntryString() {
		return buffer.toString();
	}
	public void append(String entryString) {
		this.buffer.append("\n");
		this.buffer.append(entryString);
	}

	@Override
	public int compareTo(LogEntry o) {
		return Long.compare(this.timeStamp, o.timeStamp);
	}
	
}
