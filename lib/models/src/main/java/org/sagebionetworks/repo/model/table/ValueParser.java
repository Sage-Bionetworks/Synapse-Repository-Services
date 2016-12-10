package org.sagebionetworks.repo.model.table;

/**
 * Abstraction for a parsers that convert from a String to
 * an Object that can be inserted into the database.
 *
 */
public interface ValueParser {
	
	/**
	 * Parse the given value.
	 * @param value
	 * @return
	 * @throws IllegalArgumentException
	 */
	Object parseValue(String value) throws IllegalArgumentException;

}
