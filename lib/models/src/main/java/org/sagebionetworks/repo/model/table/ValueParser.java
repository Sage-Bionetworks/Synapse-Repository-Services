package org.sagebionetworks.repo.model.table;

/**
 * Abstraction for a parsers that convert from a String to
 * an Object that can be inserted into the database.
 *
 */
public interface ValueParser {
	
	/**
	 * Parse the given value for a database write.
	 * 
	 * @param value
	 * @return
	 * @throws IllegalArgumentException
	 */
	Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException;
	

	/**
	 * Parse the given value for a database read.
	 * 
	 * @param value
	 * @return
	 * @throws IllegalArgumentException
	 */
	String parseValueForDatabaseRead(String value) throws IllegalArgumentException;
}
