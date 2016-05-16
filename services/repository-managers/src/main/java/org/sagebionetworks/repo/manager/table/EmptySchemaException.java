package org.sagebionetworks.repo.manager.table;

/**
 * Thrown when a table does not have a schema.
 * 
 *
 */
public class EmptySchemaException extends Exception {

	private static final long serialVersionUID = 1L;
	
	String tableId;

	public EmptySchemaException() {
	}

	public EmptySchemaException(String message, String tableId) {
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
