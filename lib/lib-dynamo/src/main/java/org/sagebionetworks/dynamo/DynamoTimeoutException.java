package org.sagebionetworks.dynamo;

/**
 * When Dynamo fails to complete within the specified time.
 */
public class DynamoTimeoutException extends RuntimeException {

	private static final long serialVersionUID = 7698572578705859972L;

	public DynamoTimeoutException(String msg) {
		super(msg);
	}

	public DynamoTimeoutException(String msg, Throwable e) {
		super(msg, e);
	}

	public DynamoTimeoutException(Throwable e) {
		super(e);
	}
}
