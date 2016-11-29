package org.sagebionetworks.tool.migration.v5;

public class WorkerFailedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public WorkerFailedException() {
		super();
	}

	public WorkerFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public WorkerFailedException(String message) {
		super(message);
	}

	public WorkerFailedException(Throwable cause) {
		super(cause);
	}

}