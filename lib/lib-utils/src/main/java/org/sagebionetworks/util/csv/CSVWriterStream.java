package org.sagebionetworks.util.csv;

import java.io.IOException;

/**
 * Abstraction for writing CSV data to a stream.
 * @author John
 *
 */
public interface CSVWriterStream {

	/**
	 * Write the next row to the stream.
	 * 
	 * @param nextLine
	 * @throws IOException 
	 */
	public void writeNext(String[] nextLine) throws IOException;
}
