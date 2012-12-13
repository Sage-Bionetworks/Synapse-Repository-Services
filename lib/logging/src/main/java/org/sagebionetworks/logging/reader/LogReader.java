package org.sagebionetworks.logging.reader;

import java.io.BufferedReader;
import java.io.IOException;

import org.sagebionetworks.logging.collate.LogEvent;

/**
 * An interface for defining how to read a given type of log. In order to use
 * this with the log collation utils, you must also define the LogReaderFactory
 * class.
 *
 * When reading a log file line, it's important to note that the {line} argument
 * should contain the WHOLE log file event, INCLUDING the timestamp.
 *
 * @author geoff
 *
 */
public interface LogReader {

	public interface LogReaderFactory<T extends LogReader> {
		T create(BufferedReader rdr);
	}

	/**
	 * Read the next log event from the reader.  
	 * @return LogEvent representing the next event logged in the file, or null if EndOfFile reached.
	 * @throws IOException
	 */
	public LogEvent readLogEvent() throws IOException;

}