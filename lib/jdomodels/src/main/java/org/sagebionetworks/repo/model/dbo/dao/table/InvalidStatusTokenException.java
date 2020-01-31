package org.sagebionetworks.repo.model.dbo.dao.table;

/**
 * Exception thrown when attempting to set a table/view to available/failed with
 * a token that does not match the current state. This indicates a change
 * occurred on the table/view after the worker started rebuilding.
 *
 */
public class InvalidStatusTokenException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidStatusTokenException() {
		super();
	}

	public InvalidStatusTokenException(String arg0) {
		super(arg0);
	}

	public InvalidStatusTokenException(Throwable arg0) {
		super(arg0);
	}

}
