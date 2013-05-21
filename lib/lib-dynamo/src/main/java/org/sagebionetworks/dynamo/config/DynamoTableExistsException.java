package org.sagebionetworks.dynamo.config;

/**
 * The DynamoDB table already exists and cannot be replaced.
 */
public class DynamoTableExistsException extends RuntimeException {

	private static final long serialVersionUID = 5665039279784333579L;

	public DynamoTableExistsException(String msg) {
		super(msg);
	}

	public DynamoTableExistsException(String msg, Throwable e) {
		super(msg, e);
	}

	public DynamoTableExistsException(Throwable e) {
		super(e);
	}
}
