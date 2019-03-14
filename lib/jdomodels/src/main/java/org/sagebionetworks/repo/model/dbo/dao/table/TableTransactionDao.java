package org.sagebionetworks.repo.model.dbo.dao.table;

public interface TableTransactionDao {

	/**
	 * Start a new transaction for the given table and users.
	 * 
	 * @param tableId
	 * @param userId
	 * @return Returns the transaction number for the newly created transaction.
	 */
	long startTransaction(String tableId, Long userId);

	/**
	 * Get
	 * 
	 * @param transactionId
	 * @return
	 */
	TableTransaction getTransaction(Long transactionId);

	/**
	 * Delete all transaction data for a given table.
	 * 
	 * @param tableId
	 * @return Number of transactions deleted.
	 */
	public int deleteTable(String tableId);

}
