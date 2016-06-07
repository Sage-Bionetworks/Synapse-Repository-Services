package org.sagebionetworks.repo.manager.table;

/**
 * Thrown when a table query will return no results.
 *
 */
public class EmptyResultException extends Exception {

	private static final long serialVersionUID = 1L;
	
	String tableId;

	public EmptyResultException() {
	}

	public EmptyResultException(String message, String tableId) {
		super(message);
		this.tableId = tableId;
	}

	/**
	 * This ID of the table.
	 * 
	 */
	public String getTableId() {
		return tableId;
	}
}
