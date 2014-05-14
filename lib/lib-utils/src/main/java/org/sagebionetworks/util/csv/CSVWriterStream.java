package org.sagebionetworks.util.csv;

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
	 */
	public void writeNext(String[] nextLine);
}
