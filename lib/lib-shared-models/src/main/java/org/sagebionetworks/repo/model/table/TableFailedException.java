package org.sagebionetworks.repo.model.table;

import com.google.common.base.Strings;

/**
 * This exception is thrown when a table is in a failed state.
 */
public class TableFailedException extends Exception {
	
	private static final long serialVersionUID = -4467377538941433997L;
	TableStatus status;
	
	/**
	 * Create a TableFailedException always wraps a TableStatus
	 * 
	 * @param status
	 */
	public TableFailedException(TableStatus status){
		super(extractMessage(status));
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

	private static String extractMessage(TableStatus status) {
		if (!Strings.isNullOrEmpty(status.getErrorMessage())) {
			return status.getErrorMessage();
		} else if (!Strings.isNullOrEmpty(status.getErrorDetails())) {
			return status.getErrorDetails();
		} else {
			return "Table is in a failed state";
		}
	}
}
