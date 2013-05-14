package org.sagebionetworks.tool.migration.v3.stream;

/**
 * Simple abstraction for an object writer.
 * 
 * @author jmhill
 *
 * @param <T>
 */
public interface RowWriter <T> {

	public void write(T toWrite);
}
