package org.sagebionetworks.repo.manager.table;

public interface TableTransactionContext {
	
	/**
	 * @return The id of the current table transaction
	 */
	long getTransactionId();
	
	/**
	 * @return True if a transaction was started, false otherwise
	 */
	boolean isTransactionStarted();

}
