package org.sagebionetworks.table.query.util;

/**
 * Simple aggregate queries always return one row so count queries are not necessary.
 *
 */
public class SimpleAggregateQueryException extends Exception {

	private static final long serialVersionUID = 1L;

	public SimpleAggregateQueryException() {
		super();
	}

	public SimpleAggregateQueryException(String message) {
		super(message);
	}

}
