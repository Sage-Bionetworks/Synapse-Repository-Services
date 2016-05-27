package org.sagebionetworks.repo.model.table;

/**
 * Thrown when a table lock is unavailable;
 * @author John
 *
 */
public class TableLockUnavailableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TableLockUnavailableException() {
		super();
	}

	public TableLockUnavailableException(Throwable cause) {
		super(cause);
	}


}
