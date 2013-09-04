package org.sagebionetworks.logging.s3;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;

/**
 * This reader reads log entries from a log one entry at a time.
 * 
 * @author John
 *
 */
public class LogReader {
	
	BufferedReader reader;
	LogEntry lastEntry = null;
	boolean done = false;
	
	/**
	 * This reader will read all data from the passed reader.
	 * 
	 * @param reader
	 * @param lastEntry
	 */
	public LogReader(BufferedReader reader) {
		super();
		this.reader = reader;
	}

	/**
	 * Read a single log entry from the passed reader. 
	 * @return LogEntry as long as there are entries to read.  Returns null when there are no more entries to read.
	 * @throws IOException 
	 */
	public LogEntry read() throws IOException{
		if(done) return null;
		LogEntry current = lastEntry;
		String line = null;
		while(true){
			line = reader.readLine();
			// This signals the end of the file
			if(line == null) {
				done = true;
				lastEntry = null;
				return current;
			}
			try {
				LogEntry entry = new LogEntry(line);
				if(current == null){
					current = entry;
				}else{
					// We have found the start of the next entry
					lastEntry = entry;
					break;
				}
			} catch (ParseException e) {
				if(current == null) throw new IllegalArgumentException("The first line of the log does not start with a valid log entry that starts with an ISO8601 GMT string");
				// This is not the first line of a log entry to append it to the current.
				current.append(line);
			}
		}
		// Return the current entry
		return current;
	}
	
	/**
	 * This reader must be closed when done reading from it.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException{
		reader.close();
	}

}
