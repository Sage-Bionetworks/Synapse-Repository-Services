package org.sagebionetworks.repo.model.auth;

public interface AuthenticationReceiptDAO {

	/**
	 * Answer the question, "Has this user logged in and been issue this receipt?"
	 * 
	 * @param userId
	 * @param receipt
	 * @return true if we have a record of this combination in the DB, false otherwise.
	 */
	public boolean isValidReceipt(long userId, String receipt);

	/**
	 * Create a new receipt for this user
	 * 
	 * @param userId
	 * @return the newly created receipt
	 */
	public String createNewReceipt(long userId);

	/**
	 * Create a new receipt for that will expire after expirationPeriodMillis milliseconds.
	 *
	 * @param userId
	 * @param expirationPeriodMillis
	 * @return the newly created receipt
	 */
	public String createNewReceipt(long userId, long expirationPeriodMillis);

	/**
	 * Replace the old receipt with a new one
	 * 
	 * @param userId
	 * @param oldReceipt
	 * @return the new receipt
	 */
	public String replaceReceipt(long userId, String oldReceipt);

	/**
	 * Get the number of receipts a user has
	 * 
	 * @param userId
	 * @return
	 */
	public long countReceipts(long userId);

	/**
	 * Remove all receipts that is older than expirationTime
	 * 
	 * @param expirationTime
	 */
	public void deleteExpiredReceipts(long userId, long expirationTime);

	/**
	 * Remove all data from AuthenticationReceip table.
	 * Only use this for testing.
	 */
	public void truncateAll();
}
