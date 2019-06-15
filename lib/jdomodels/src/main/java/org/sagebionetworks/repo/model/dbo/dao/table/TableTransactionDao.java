package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Optional;

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
	 * Start a new transaction for the given table, user, and started on time.
	 * 
	 * @param tableId
	 * @param userId
	 * @param startedOn
	 * @return
	 */
	long startTransaction(String tableId, Long userId, Long startedOn);

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

	/**
	 * Get the tableId associated with the given transaction ID and lock the
	 * transaction row (SELECT FOR UPDATE).
	 * 
	 * @param transactionId
	 * @return
	 */
	long getTableIdWithLock(long transactionId);

	/**
	 * Link a transaction to a version.
	 * 
	 * @param transactionId
	 * @param version
	 * @return
	 */
	void linkTransactionToVersion(long transactionId, long version);

	/**
	 * Update the etag of a transaction.
	 * 
	 * @param transactionId
	 * @return The new etag.
	 */
	String updateTransactionEtag(long transactionId);

	/**
	 * Get the transaction Id for a table version.
	 * 
	 * @param tableId
	 * @param version
	 * @return If there is a transaction for the given table and version then the
	 *         transaction ID will be returned. Optional.empty() if such a
	 *         transaction does not exist.
	 * 
	 */
	Optional<Long> getTransactionForVersion(String tableId, long version);

}
