package org.sagebionetworks.repo.manager.table;

/**
 * Thrown when a table index connection is unavailable.
 * 
 */
public class TableIndexConnectionUnavailableException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TableIndexConnectionUnavailableException(String message) {
		super(message);
	}

}
