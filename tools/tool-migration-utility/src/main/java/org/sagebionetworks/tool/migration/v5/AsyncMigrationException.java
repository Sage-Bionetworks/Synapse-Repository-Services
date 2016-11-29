package org.sagebionetworks.tool.migration.v5;

public class AsyncMigrationException extends RuntimeException {

	private static final long serialVersionUID = 83885077L;

	public AsyncMigrationException() {
		super();
	}

	public AsyncMigrationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AsyncMigrationException(String message) {
		super(message);
	}

	public AsyncMigrationException(Throwable cause) {
		super(cause);
	}

}