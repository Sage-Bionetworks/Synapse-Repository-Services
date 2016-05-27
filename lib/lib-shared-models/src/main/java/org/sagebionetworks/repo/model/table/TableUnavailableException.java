package org.sagebionetworks.repo.model.table;

/**
 * This exception is thrown when a table is unavailable for a consistent read.
 * 
 * @author jmhill
 *
 */
public class TableUnavailableException extends Exception {
	
	private static final long serialVersionUID = 6302717930229706556L;
	TableStatus status;
	
	/**
	 * Create a TableUnavilableException always wraps a TableStatus
	 * 
	 * @param status
	 */
	public TableUnavailableException(TableStatus status){
		this.status = status;
	}

	/**
	 * When this exception is thrown it will always include the status of the table.
	 * 
	 * @return
	 */
	public TableStatus getStatus() {
		return status;
	}

}
